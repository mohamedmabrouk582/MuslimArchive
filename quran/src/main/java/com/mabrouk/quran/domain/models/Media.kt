package com.mabrouk.quran.domain.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Media(
    val url:String,
    val provider:String,
    @SerializedName("author_name")
    val authorName:String
) : Parcelable
