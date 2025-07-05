package com.mabrouk.quran.data.repository

import android.content.Context
import android.util.Log
import com.mabrouk.core.network.Result
import com.mabrouk.core.network.executeCall
import com.mabrouk.core.network.executeCall2
import com.mabrouk.core.network.toArrayList
import com.mabrouk.quran.data.api.*
import com.mabrouk.quran.data.db.QuranDao
import com.mabrouk.quran.domain.models.QuranReader
import com.mabrouk.quran.domain.models.Surah
import com.mabrouk.quran.domain.models.TafsirAya
import com.mabrouk.quran.domain.models.Verse
import com.mabrouk.quran.domain.repository.SurahDefaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.ResponseBody
import javax.inject.Inject

/**
 * @name Mohamed Mabrouk
 * Copyright (c) 4/16/22
 */
class SurahRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val quranApi: QuranApi,
    private val api: TafseerApi,
    private val dao: QuranDao
)  : SurahDefaultRepository {


    override  fun requestTafsir(
        chapterId: Int,
        verseId: Int,
        id: Int
    ): Flow<Result<TafsirAya>> {
        return executeCall2(context = context) { api.getAyaTafseer(id,chapterId,verseId) }
    }

    override suspend fun saveTafsir(data: TafsirAya) {
        dao.saveVerseTafsir(data)
    }

    override fun getSavedTafsir(chapterId: Int, verseId: Int): Flow<List<TafsirAya>> {
        return dao.getSavedTafsir("/quran/$chapterId/$verseId/")
    }

    override  fun downloadAudio(url: String): Flow<Result<ResponseBody>> {
        return executeCall(context){
            quranApi.downloadAudio(url)
        }
    }

    override suspend fun updateSurahAudioDownload(id: Int, isDownload: Boolean) {
        dao.updateSurahAudioDownload(id,isDownload)
    }


    override fun getReader(id: Int): Flow<QuranReader> {
        return dao.getReader(id)
    }

    override  fun getReaders(): Flow<List<QuranReader>> {
        return dao.getAllReaders()
    }

    override suspend fun updateQuranReader(item: QuranReader) {
        Log.d("ededefefefe",item.versesIds.toString())
        return dao.updateReader(item)
    }

    override fun getSavedVerses(chapterNumber: Int): Flow<List<Verse>> {
        return dao.getSaveVerses(chapterNumber)
    }

    override suspend fun getFileSize(url: String): Long {
        val response = quranApi.getContentLength(url)
        try {
            if (response.isSuccessful){
                val contentLength = response.headers()["Content-Length"]
                return contentLength?.toLongOrNull()?:0L
            }
        }catch (E:Exception){
           return 0L
        }
        return 0L
    }

}