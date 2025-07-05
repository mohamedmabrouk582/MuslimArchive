package com.mabrouk.quran.data.db

import android.util.Log
import androidx.room.*
import com.mabrouk.quran.domain.models.*
import com.mabrouk.quran.presentaion.utils.mapJuz
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReaders(readers : ArrayList<QuranReader>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSurahs(suras:ArrayList<Surah>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveJuzs(juzs:ArrayList<Juz>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVerses(vers:ArrayList<Verse>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVerseTafsir(tafsirEntities:TafsirAya)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateJUz(juz:Juz)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateVerse(verse:Verse)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateReader(readerEntity: QuranReader)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSurah(sura:Surah)

    @Query("update surah set audiosDownloaded=:bool , tafsirDownloaded=:bool where id=:id")
    suspend fun updateSurahAudioDownload(id:Int,bool:Boolean) : Int


    @Query("update surah set isDownload=:bool where id=:id")
    suspend fun updateSurahDownload(id:Int,bool:Boolean) : Int


    @Query("SELECT CASE WHEN (SELECT COUNT(*) from surah where id in (:surahIds) AND isDownload = 0 ) = 0   THEN 1 ELSE 0 END")
    suspend fun areAllSurahsDownloaded(surahIds: List<Int>): Boolean

    @Query("SELECT * FROM Juz")
    fun getAllJuz(): List<Juz>

    @Query("SELECT * FROM Surah")
    fun getAllSurahs(): List<Surah>

    @Query("SELECT COUNT(*) FROM Surah")
    fun isSurahTableEmpty(): Flow<Int>

    @Transaction
    fun getJuzSurahs(): List<JuzSurah> {
        val allJuz = getAllJuz()
        val allSurahs = getAllSurahs()

        return mapJuz(allJuz, allSurahs)
    }


    @Query("select * from QuranReader")
    fun getAllReaders () : Flow<List<QuranReader>>

    @Query("select * from QuranReader where readerId =:id")
    fun getReader(id:Int) : Flow<QuranReader>

    @Query("select * from surah where id=:id")
    fun getSurahById(id:Int) : Flow<Surah>

    @Query("select * from surah")
    fun getSavedSurah() : Flow<List<Surah>>

    @Query("select * from juz")
    fun getSavedJuzs() : Flow<List<Juz>>

    @Query("select * from verse where chapterId =:id")
    fun getSaveVerses(id:Int) : Flow<List<Verse>>

    @Query("select * from tafsiraya where verseKey=:key ")
    fun getSavedTafsir(key:String) : Flow<List<TafsirAya>>


    @Query("select * from surah_fts where surah_fts MATCH :search")
    fun searchByAtSurah(search:String) : Flow<List<SurahFts>>

    @Query("delete from surah")
    fun deleteAllSurahs()

    @Query("delete from juz")
    fun deleteAllJuz()

}