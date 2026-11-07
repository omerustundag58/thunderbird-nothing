package com.fsck.k9.ui.localai

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val localAiModule = module {
    single { LocalAiPreferences(context = androidContext()) }
    single { LocalModelManager(context = androidContext()) }
    single { LocalLlmEngine(context = androidContext()) }
    single { CategoryStore(context = androidContext()) }
    single { CategoryClassifier(engine = get()) }
    single {
        LocalAiRepository(
            accountManager = get(),
            messageListRepository = get(),
            messageStoreManager = get(),
            categoryStore = get(),
            preferences = get(),
            modelManager = get(),
            classifier = get(),
            engine = get(),
        )
    }

    viewModel {
        LocalAiCategoriesViewModel(
            repository = get(),
            modelManager = get(),
        )
    }
}
