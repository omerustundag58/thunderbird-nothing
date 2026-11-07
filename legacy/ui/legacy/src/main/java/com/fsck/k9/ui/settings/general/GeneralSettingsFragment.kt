package com.fsck.k9.ui.settings.general

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import androidx.work.WorkInfo
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import app.k9mail.feature.telemetry.api.TelemetryManager
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.ui.BuildConfig
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.extensions.withArguments
import com.fsck.k9.ui.localai.LocalAiCategoriesActivity
import com.fsck.k9.ui.localai.LocalAiPreferences
import com.fsck.k9.ui.localai.LocalModelManager
import com.fsck.k9.ui.observe
import com.fsck.k9.ui.settings.notificationactions.NotificationActionsSettingsActivity
import com.fsck.k9.ui.settings.remove
import com.google.android.material.snackbar.Snackbar
import com.takisoft.preferencex.PreferenceFragmentCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: GeneralSettingsViewModel by viewModel()
    private val dataStore: GeneralSettingsDataStore by inject()
    private val telemetryManager: TelemetryManager by inject()
    private val featureFlagProvider: FeatureFlagProvider by inject()
    private val jobManager: K9JobManager by inject()
    private val localAiPreferences: LocalAiPreferences by inject()
    private val localModelManager: LocalModelManager by inject()

    private var rootKey: String? = null
    private var currentUiState: GeneralSettingsUiState? = null
    private var snackbar: Snackbar? = null

    private val exportLogsResultContract = registerForActivityResult(CreateDocument("text/plain")) { contentUri ->
        if (contentUri != null) {
            viewModel.exportLogs(contentUri)
        }
    }

    private val importModelResultContract = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importLocalModel(uri)
        }
    }

    private val exportSyncDebugLogsResultContract =
        registerForActivityResult(CreateDocument("text/plain")) { contentUri ->
            if (contentUri != null) {
                viewModel.fileExport(contentUri.toString())
            }
        }
    private val choseFileSyncDebugLogsResultContract =
        registerForActivityResult(CreateDocument("text/plain")) { contentUri ->
            if (contentUri != null) {
                context?.contentResolver?.takePersistableUriPermission(
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                jobManager.scheduleDebugLogLimit(contentUri.toString()).observe(
                    this,
                ) { workInfo: WorkInfo? ->
                    if (workInfo != null) {
                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            viewModel.showExportSnackbar(true)
                        } else if (workInfo.state == WorkInfo.State.FAILED) {
                            viewModel.showExportSnackbar(false)
                        }
                    }
                }
            }
        }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore
        this.rootKey = rootKey
        setPreferencesFromResource(R.xml.general_settings, rootKey)

        initializeLocalAiPreferences()

        findPreference<Preference>("notification_actions_settings")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                context?.let { NotificationActionsSettingsActivity.start(it) }
                true
            }
        findPreference<Preference>("remote_feature_flag")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                FeatureLauncherActivity.launch(
                    context = requireContext(),
                    target = FeatureLauncherTarget.RemoteFeatureFlag,
                )
                true
            }

        findPreference<Preference>("battery_optimization")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                openBatteryOptimizationSettings()
                true
            }

        val listener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (!(newValue as Boolean)) {
                jobManager.cancelDebugLogLimit()
                exportSyncDebugLogsResultContract.launch(formatFileExportUriString())
            } else {
                choseFileSyncDebugLogsResultContract.launch(formatFileExportUriString())
            }
            true
        }
        findPreference<Preference>("sync_debug_logging")?.onPreferenceChangeListener = listener
        featureFlagProvider.provide(GeneratedFeatureFlagKey.DISABLE_FONT_SIZE_CONFIG)
            .onEnabled {
                val parentPreference = findPreference<PreferenceCategory>("global_preferences")
                val fontSizePreferenceScreen = findPreference<PreferenceScreen>("font_size")

                if (parentPreference != null && fontSizePreferenceScreen != null) {
                    parentPreference.removePreference(fontSizePreferenceScreen)
                }
            }

        findPreference<Preference>("debug_secret_debug_screen")?.apply {
            if (!BuildConfig.DEBUG) {
                remove()
                onPreferenceClickListener = null
            } else {
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    viewModel.onOpenSecretDebugScreen(requireContext())

                    true
                }
            }
        }

        initializeDataCollection()

        viewModel.uiState.observe(this) { uiState ->
            updateUiState(uiState)
        }

        viewModel.modelImportState.observe(this) { importState ->
            updateModelImportState(importState)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissSnackbar()
    }

    override fun onResume() {
        super.onResume()
        updateBatteryOptimizationSummary()
        updateModelStatusSummary()
    }

    private fun initializeLocalAiPreferences() {
        findPreference<SwitchPreferenceCompat>("local_ai_enabled")?.apply {
            isChecked = localAiPreferences.isLlmEnabled
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                localAiPreferences.isLlmEnabled = newValue as Boolean
                updateModelStatusSummary()
                true
            }
        }

        findPreference<Preference>("local_ai_import_model")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                importModelResultContract.launch(arrayOf("*/*"))
                true
            }

        findPreference<Preference>("local_ai_model_status")?.isSelectable = false
        findPreference<Preference>("local_ai_info")?.isSelectable = false

        findPreference<Preference>("local_ai_categories")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                LocalAiCategoriesActivity.start(requireContext())
                true
            }

        findPreference<Preference>("local_ai_clear_cache")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                viewModel.clearCategoryCache()
                Toast.makeText(requireContext(), R.string.local_ai_cache_cleared, Toast.LENGTH_SHORT).show()
                true
            }

        updateModelStatusSummary()
    }

    private fun updateModelStatusSummary() {
        findPreference<Preference>("local_ai_model_status")?.summary = localModelManager.describeStatus()
    }

    private fun updateModelImportState(importState: ModelImportState) {
        when (importState) {
            ModelImportState.Idle -> Unit

            ModelImportState.Importing -> {
                findPreference<Preference>("local_ai_model_status")?.summary =
                    getString(R.string.local_ai_model_importing)
            }

            ModelImportState.Success -> {
                updateModelStatusSummary()
                Toast.makeText(requireContext(), R.string.local_ai_model_import_success, Toast.LENGTH_LONG).show()
                viewModel.consumeModelImportState()
            }

            is ModelImportState.Failure -> {
                updateModelStatusSummary()
                Toast.makeText(requireContext(), R.string.local_ai_model_import_failure, Toast.LENGTH_LONG).show()
                viewModel.consumeModelImportState()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = preferenceScreen.title

        val menuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    if (rootKey == PREFERENCE_SCREEN_DEBUGGING) {
                        menuInflater.inflate(R.menu.debug_settings_option, menu)
                    }
                }

                override fun onPrepareMenu(menu: Menu) {
                    if (rootKey == PREFERENCE_SCREEN_DEBUGGING) {
                        currentUiState?.let { uiState ->
                            menu.findItem(R.id.exportLogs).isEnabled = uiState.isExportLogsMenuEnabled
                        }
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.exportLogs -> {
                            exportLogsResultContract.launch(GeneralSettingsViewModel.DEFAULT_FILENAME)
                            true
                        }

                        R.id.exportSyncLogs -> {
                            exportSyncDebugLogsResultContract.launch(formatFileExportUriString())
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }

    private fun initializeDataCollection() {
        if (!telemetryManager.isTelemetryFeatureIncluded()) {
            findPreference<Preference>(PREFERENCE_DATA_COLLECTION)?.remove()
        }
    }

    private fun updateUiState(uiState: GeneralSettingsUiState) {
        val oldUiState = currentUiState
        currentUiState = uiState

        if (oldUiState?.isExportLogsMenuEnabled != uiState.isExportLogsMenuEnabled) {
            setExportLogsMenuEnabled()
        }

        if (oldUiState?.snackbarState != uiState.snackbarState) {
            setSnackbarState(uiState.snackbarState)
        }
    }

    private fun setExportLogsMenuEnabled() {
        requireActivity().invalidateOptionsMenu()
    }

    private fun setSnackbarState(snackbarState: SnackbarState) {
        when (snackbarState) {
            SnackbarState.Hidden -> dismissSnackbar()
            SnackbarState.ExportLogSuccess -> showSnackbar(R.string.debug_export_logs_success)
            SnackbarState.ExportLogFailure -> showSnackbar(R.string.debug_export_logs_failure)
        }
    }

    private fun dismissSnackbar() {
        snackbar?.dismiss()
        snackbar = null
    }

    private fun showSnackbar(message: Int) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_INDEFINITE)
            .also { snackbar = it }
            .show()
    }

    private fun formatFileExportUriString(): String {
        val now = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%s_%s.txt",
            DEFAULT_SYNC_FILENAME,
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time),
        )
    }

    private fun updateBatteryOptimizationSummary() {
        val preference = findPreference<Preference>("battery_optimization") ?: return
        val powerManager = requireContext().getSystemService(PowerManager::class.java)
        val unrestricted = powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
        preference.summary = getString(
            if (unrestricted) {
                R.string.battery_optimization_summary_on
            } else {
                R.string.battery_optimization_summary_off
            },
        )
    }

    private fun openBatteryOptimizationSettings() {
        val context = requireContext()
        val powerManager = context.getSystemService(PowerManager::class.java)
        val unrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        val intent = if (unrestricted) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            )
        }
        runCatching { startActivity(intent) }.onFailure {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }
    }

    companion object {
        private const val PREFERENCE_SCREEN_DEBUGGING = "debug_preferences"
        private const val PREFERENCE_DATA_COLLECTION = "data_collection"
        const val DEFAULT_SYNC_FILENAME = "thunderbird-sync-logs"

        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments {
            putString(ARG_PREFERENCE_ROOT, rootKey)
        }
    }
}
