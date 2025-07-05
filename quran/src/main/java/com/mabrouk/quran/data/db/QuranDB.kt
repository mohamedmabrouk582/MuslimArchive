package com.mabrouk.quran.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mabrouk.quran.domain.models.*

@Database(
    entities = [
        Surah::class,
        Juz::class,
        Verse::class,
        TafsirAya::class,
        QuranReader::class ,
        SurahFts::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DbConverter::class)
abstract class QuranDB : RoomDatabase() {
    abstract fun getQuranDao(): QuranDao
}