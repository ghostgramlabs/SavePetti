package com.ghostgramlabs.pettibox.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ghostgramlabs.pettibox.data.local.CategoryEntity
import com.ghostgramlabs.pettibox.data.local.SaveItemEntity
import com.ghostgramlabs.pettibox.data.ocr.OcrWorkTags
import com.ghostgramlabs.pettibox.data.preferences.OnboardingPreferences
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import com.ghostgramlabs.pettibox.domain.model.SourceApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceCount(val source: String, val emoji: String, val display: String, val count: Int)

data class HomeState(
    val isLoading: Boolean = true,
    val recent: List<SaveItemEntity> = emptyList(),
    val pinned: List<SaveItemEntity> = emptyList(),
    val favorites: List<SaveItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val sources: List<SourceCount> = emptyList(),
    val totalCount: Int = 0,
    val isIndexingText: Boolean = false,
    val showOnboarding: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SaveRepository,
    private val onboardingPreferences: OnboardingPreferences,
    @ApplicationContext appContext: Context
) : ViewModel() {
    private val isTextIndexing = textIndexingFlow(appContext)

    /**
     * Each component flow is bounded — recent/pinned/favorites are LIMITed in
     * SQL, sources comes from a GROUP BY aggregate, total is a COUNT(*). The
     * home screen never loads the full row set, even at 100k saves.
     */
    val state: StateFlow<HomeState> = combine(
        repo.observeRecent(20),
        repo.observePinned(),
        repo.observeFavorites(),
        repo.observeCategories(),
        repo.observeSourceCounts(),
        repo.observeTotal(),
        isTextIndexing,
        onboardingPreferences.showHomeOnboarding
    ) { args ->
        // combine(vararg) returns Array<Any?> at runtime — the per-cast
        // unchecked-cast warnings are unavoidable and noisy; we accept the
        // type contract because the call site is the only one constructing
        // and consuming the array.
        @Suppress("UNCHECKED_CAST")
        val recent = args[0] as List<SaveItemEntity>
        @Suppress("UNCHECKED_CAST")
        val pinned = args[1] as List<SaveItemEntity>
        @Suppress("UNCHECKED_CAST")
        val favs = args[2] as List<SaveItemEntity>
        @Suppress("UNCHECKED_CAST")
        val cats = args[3] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val rawCounts = args[4] as List<com.ghostgramlabs.pettibox.data.local.SourceCount>
        val total = args[5] as Int
        val indexing = args[6] as Boolean
        val showOnboarding = args[7] as Boolean

        val sources = rawCounts.mapNotNull { sc ->
            val sa = runCatching { SourceApp.valueOf(sc.source) }.getOrNull() ?: return@mapNotNull null
            SourceCount(sa.name, sa.emoji, sa.displayName, sc.count)
        }.take(8)

        HomeState(
            isLoading = false,
            recent = recent,
            pinned = pinned,
            favorites = favs,
            categories = cats,
            sources = sources,
            totalCount = total,
            isIndexingText = indexing,
            showOnboarding = showOnboarding
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun toggleFavorite(item: SaveItemEntity) = viewModelScope.launch {
        repo.setFavorite(item.id, !item.isFavorite)
    }

    suspend fun exportBackupJson(): String = repo.exportBackupJson()

    fun completeOnboarding() = viewModelScope.launch {
        onboardingPreferences.markHomeOnboardingComplete()
    }
}

private fun textIndexingFlow(context: Context): Flow<Boolean> = flow {
    val workManager = WorkManager.getInstance(context)
    while (true) {
        val infos = workManager.getWorkInfosByTag(OcrWorkTags.TEXT_INDEXING).get()
        emit(
            infos.any {
                it.state == WorkInfo.State.ENQUEUED ||
                    it.state == WorkInfo.State.RUNNING ||
                    it.state == WorkInfo.State.BLOCKED
            }
        )
        delay(1_250)
    }
}.distinctUntilChanged().flowOn(Dispatchers.IO)
