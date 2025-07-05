package com.mabrouk.quran.domain.repository

import com.mabrouk.core.network.Result
import com.mabrouk.quran.domain.models.QuranReader
import com.mabrouk.quran.domain.models.Surah
import com.mabrouk.quran.domain.models.TafsirAya
import com.mabrouk.quran.domain.models.Verse
import kotlinx.coroutines.flow.Flow
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * @name Mohamed Mabrouk
 * Copyright (c) 4/16/22
 */
interface SurahDefaultRepository {
     fun requestTafsir(chapterId:Int , verseId:Int,id:Int) : Flow<Result<TafsirAya>>
    suspend fun saveTafsir(data:TafsirAya)
    fun getSavedTafsir(chapterId: Int,verseId: Int) : Flow<List<TafsirAya>>
     fun downloadAudio(url:String) : Flow<Result<ResponseBody>>
    suspend fun updateSurahAudioDownload(id:Int,isDownload:Boolean)
    fun getReader(id:Int):Flow<QuranReader>
    fun getReaders() : Flow<List<QuranReader>>
    suspend fun updateQuranReader(item:QuranReader)
    fun getSavedVerses(chapterNumber:Int) : Flow<List<Verse>>
    suspend fun getFileSize(url: String) : Long
}