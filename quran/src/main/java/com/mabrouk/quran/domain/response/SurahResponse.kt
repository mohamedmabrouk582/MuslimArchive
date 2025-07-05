package com.mabrouk.quran.domain.response

import com.mabrouk.quran.domain.models.Surah


/**
 * @name Mohamed Mabrouk
 * Copyright (c) 4/16/22
 */
data class SurahResponse(
    val chapters:ArrayList<Surah>
)
