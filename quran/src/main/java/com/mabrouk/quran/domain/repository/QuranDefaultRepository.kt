package com.mabrouk.quran.domain.repository

import com.mabrouk.core.network.Result
import com.mabrouk.quran.domain.models.Juz
import com.mabrouk.quran.domain.models.JuzSurah
import com.mabrouk.quran.domain.models.QuranReader
import com.mabrouk.quran.domain.models.Surah
import com.mabrouk.quran.domain.models.SurahFts
import com.mabrouk.quran.domain.models.Verse
import com.mabrouk.quran.domain.response.JuzResponse
import com.mabrouk.quran.domain.response.SurahResponse
import com.mabrouk.quran.domain.response.VersesResponse
import kotlinx.coroutines.flow.Flow

/**
 * @name Mohamed Mabrouk
 * Copyright (c) 4/16/22
 */
interface QuranDefaultRepository {
     fun requestJuz() : Flow<Result<JuzResponse>>
     fun requestSurahs() : Flow<Result<SurahResponse>>
     fun requestVerses(chapterNumber:Int, pageNumber:Int=0) : Flow<Result<VersesResponse>>
    suspend fun saveJuz(juzes:ArrayList<Juz>)
    suspend fun saveSurahs(surahs : ArrayList<Surah>)
    suspend fun saveVerses(verses : ArrayList<Verse>)
    fun getSavedSurah() : Flow<List<Surah>>
    fun getSavedJuz() : Flow<List<Juz>>
    fun getSurahById(surahId:Int) : Flow<Surah>
    suspend fun updateJuz(juzes: Juz)
    suspend fun updateSura(surah: Surah)
    suspend fun insertReaders(readers:ArrayList<QuranReader>)
    suspend fun updateReader(readers:QuranReader)
    fun searchBySurah(query:String) : Flow<List<SurahFts>>
    suspend fun isSurahTableEmpty() : Boolean
    fun getJuzSurahs() : List<JuzSurah>
    suspend fun updateVerseDownload(isDownload:Boolean,id:Int)

    suspend fun updateAudioDownload(isDownload:Boolean,id:Int)
    suspend fun areAllSurahsDownloaded(surahIds: List<Int>): Boolean

}