package com.mabrouk.quran.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class AyaTafsirs(val name:String,val tafsirs:ArrayList<TafsirAya>): Parcelable