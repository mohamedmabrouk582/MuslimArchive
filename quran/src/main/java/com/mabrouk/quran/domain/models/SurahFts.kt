package com.mabrouk.quran.domain.models

import androidx.room.Entity
import androidx.room.Fts4
import com.mabrouk.quran.domain.models.Surah

/**
 * @name Mohamed Mabrouk
 * Copyright (c) 10/5/22
 */
@Entity(tableName = "surah_fts")
@Fts4(contentEntity = Surah::class)
data class SurahFts(
        val id: Int,
        val nameComplex: String? = null,
        val nameArabic: String? = null,
        val nameSimple: String? = null,
        var isDownload: Boolean = false ,
        var  fromTo: String? = null
)