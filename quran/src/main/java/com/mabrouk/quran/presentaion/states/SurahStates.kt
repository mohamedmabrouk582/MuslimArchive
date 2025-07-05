package com.mabrouk.quran.presentaion.states

import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import com.mabrouk.quran.domain.models.QuranReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @name Mohamed Mabrouk
 * Copyright (c) 4/17/22
 */
sealed class SurahStates{
    data object IDLE : SurahStates()
    data class DownloadVerse(val workInfo: Flow<WorkInfo?>) : SurahStates()
    data class UpdateReader(val readerEntity: QuranReader) : SurahStates()
}

sealed class VersesDownloadStates{
    data object RUNNING: VersesDownloadStates()
    data object SUCCESS: VersesDownloadStates()
    data object IDLE: VersesDownloadStates()
}
