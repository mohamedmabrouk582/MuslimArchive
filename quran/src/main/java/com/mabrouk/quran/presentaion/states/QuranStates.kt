package com.mabrouk.quran.presentaion.states

import com.mabrouk.quran.domain.models.JuzSurah
import com.mabrouk.quran.domain.models.Surah
import kotlinx.serialization.Serializable

/**
 * @name Mohamed Mabrouk
 * Copyright (c) 4/16/22
 */
sealed class QuranStates{
    data object IDLE : QuranStates()
    data object Loading : QuranStates()
    data class LoadJuzSurahs(val juzSurah: ArrayList<JuzSurah>) : QuranStates()
    data class Error(val error:String) : QuranStates()
    data class SearchResult(val juzSurah: ArrayList<JuzSurah>) : QuranStates()
    data object ClearSearch : QuranStates()
}

sealed class DownloadStates{
    data object Idle : DownloadStates()
    data object Loading : DownloadStates()
    data class Success(val ids:List<Int>) : DownloadStates()
    data object Error : DownloadStates()
}

@Serializable
object QuranList

@Serializable
data class SurahContent(val id:Int,val name:String)

