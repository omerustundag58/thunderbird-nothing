package com.fsck.k9.ui.localai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.fsck.k9.ui.observe
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Lists the nine virtual categories with counts and sample subjects. When started with [EXTRA_CATEGORY] it shows the
 * cached messages of that category instead.
 */
class LocalAiCategoriesActivity : BaseActivity() {
    private val viewModel: LocalAiCategoriesViewModel by viewModel()

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var scanButton: Button
    private lateinit var clearButton: Button

    private var selectedCategory: EmailCategory? = null

    private val categoryAdapter = CategorySummaryAdapter { category ->
        start(this, category)
    }
    private val messageAdapter = CachedMessageAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_local_ai_categories)

        selectedCategory = EmailCategory.fromApiName(intent.getStringExtra(EXTRA_CATEGORY))

        initializeViews()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = selectedCategory?.let { getString(it.titleRes) }
                ?: getString(R.string.local_ai_categories_screen_title)
        }

        if (selectedCategory == null) {
            showCategoriesMode()
        } else {
            showMessagesMode(selectedCategory!!)
        }

        viewModel.state.observe(this) { state ->
            render(state)
        }
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.categoryList)
        emptyView = findViewById(R.id.emptyView)
        scanButton = findViewById(R.id.scanButton)
        clearButton = findViewById(R.id.clearButton)
    }

    private fun showCategoriesMode() {
        recyclerView.adapter = categoryAdapter

        scanButton.setOnClickListener {
            viewModel.scan()
        }
        clearButton.setOnClickListener {
            viewModel.clearCache()
            Toast.makeText(this, R.string.local_ai_cache_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMessagesMode(category: EmailCategory) {
        scanButton.isVisible = false
        clearButton.isVisible = false

        recyclerView.adapter = messageAdapter
        val messages = viewModel.messagesFor(category)
        messageAdapter.submit(messages)
        emptyView.isVisible = messages.isEmpty()
        emptyView.text = getString(R.string.local_ai_category_empty)
        statusText.text = resources.getQuantityString(
            R.plurals.local_ai_message_count,
            messages.size,
            messages.size,
        )
    }

    private fun render(state: LocalAiUiState) {
        if (selectedCategory != null) return

        categoryAdapter.submit(state.overview)
        progressBar.isVisible = state.isScanning
        scanButton.isEnabled = !state.isScanning
        clearButton.isEnabled = !state.isScanning

        val total = state.overview.sumOf { it.count }
        emptyView.isVisible = false

        statusText.text = when {
            state.errorMessage != null -> {
                getString(R.string.local_ai_scan_failed, state.errorMessage.orEmpty())
            }

            state.isScanning -> {
                val progress = state.progress
                if (progress == null) {
                    getString(R.string.local_ai_status_starting)
                } else {
                    getString(
                        R.string.local_ai_status_scanning,
                        progress.processed,
                        progress.total,
                    )
                }
            }

            state.lastSummary != null -> {
                val summary = state.lastSummary!!
                getString(
                    R.string.local_ai_status_summary,
                    summary.scannedMessages,
                    summary.classifiedByRules,
                    summary.classifiedByLanguageModel,
                )
            }

            total > 0 -> {
                getString(R.string.local_ai_status_cached, total)
            }

            else -> {
                getString(R.string.local_ai_status_idle)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val EXTRA_CATEGORY = "category"

        fun start(context: Context, category: EmailCategory? = null) {
            val intent = Intent(context, LocalAiCategoriesActivity::class.java)
            if (category != null) {
                intent.putExtra(EXTRA_CATEGORY, category.apiName)
            }
            context.startActivity(intent)
        }
    }
}
