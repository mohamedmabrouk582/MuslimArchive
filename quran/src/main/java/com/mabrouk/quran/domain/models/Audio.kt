package com.mabrouk.quran.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Audio(
    val url:String,
    val duration:Int,
    val format:String
) : Parcelable
