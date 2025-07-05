package com.mabrouk.quran.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import com.mabrouk.core.network.Result
import com.mabrouk.core.network.executeCall
import com.mabrouk.quran.data.api.QuranApi
import com.mabrouk.quran.data.db.QuranDao
import com.mabrouk.quran.domain.models.Juz
import com.mabrouk.quran.domain.models.JuzSurah
import com.mabrouk.quran.domain.models.QuranReader
import com.mabrouk.quran.domain.models.Surah
import com.mabrouk.quran.domain.models.SurahFts
import com.mabrouk.quran.domain.models.Verse
import com.mabrouk.quran.domain.repository.QuranDefaultRepository
import com.mabrouk.quran.domain.response.JuzResponse
import com.mabrouk.quran.domain.response.SurahResponse
import com.mabrouk.quran.domain.response.VersesResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

/**
 * @name Mohamed Mabrouk
 * Copyright (c) 4/16/22
 */
class QuranRepository @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val api: QuranApi,
    private val dao: QuranDao
) : QuranDefaultRepository {

    override  fun requestJuz(): Flow<Result<JuzResponse>> {
        return executeCall(context){ api.getJuzs() }
    }

    override  fun requestSurahs(): Flow<Result<SurahResponse>> {
        return executeCall(context){ api.getAllSurah() }
    }

    override  fun requestVerses(chapterId: Int, page: Int): Flow<Result<VersesResponse>> {
        return executeCall(context){ api.getSurahVerses(chapterId, page) }
    }

    override suspend fun saveJuz(juz: ArrayList<Juz>) {
        dao.saveJuzs(juz)
    }

    override suspend fun saveSurahs(surahs: ArrayList<Surah>) {
        dao.saveSurahs(surahs)
    }

    override suspend fun saveVerses(verses: ArrayList<Verse>) {
        dao.saveVerses(verses)
    }

    override fun getSavedSurah(): Flow<List<Surah>> {
       return dao.getSavedSurah()
    }

    override fun getSavedJuz(): Flow<List<Juz>> {
        return dao.getSavedJuzs()
    }

    override fun getSurahById(id: Int): Flow<Surah> {
        return dao.getSurahById(id)
    }

    override suspend fun updateJuz(juz: Juz) {
        dao.updateJUz(juz)
    }

    override suspend fun updateSura(sura: Surah) {
        dao.updateSurah(sura)
    }

    override suspend fun insertReaders(readers: ArrayList<QuranReader>) {
        dao.insertAllReaders(readers)
    }

    override suspend fun updateReader(readers: QuranReader) {
        dao.updateReader(readers)
    }

    override  fun searchBySurah(query: String): Flow<List<SurahFts>> {
        return dao.searchByAtSurah(query.let { "*$it*" })
    }

    override suspend fun isSurahTableEmpty(): Boolean {
        return (dao.isSurahTableEmpty().firstOrNull() ?: 0) <= 0
    }


    override fun getJuzSurahs(): List<JuzSurah> {
        return dao.getJuzSurahs()
    }

    override suspend fun updateVerseDownload(isDownload: Boolean,id: Int) {
         dao.updateSurahDownload(id,isDownload)
    }

    override suspend fun updateAudioDownload(isDownload: Boolean, id: Int) {
        dao.updateSurahAudioDownload(id,isDownload)
    }

    override suspend fun areAllSurahsDownloaded(surahIds: List<Int>): Boolean {
        return dao.areAllSurahsDownloaded(surahIds)
    }

}