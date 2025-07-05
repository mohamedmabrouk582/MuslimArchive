package com.mabrouk.quran.presentaion.workers

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.mabrouk.core.BuildConfig
import com.mabrouk.core.di.IoDispatcher
import com.mabrouk.core.network.Result.*
import com.mabrouk.core.network.decimalFormat
import com.mabrouk.core.utils.FileUtils
import com.mabrouk.core.utils.FileUtils.getFile
import com.mabrouk.quran.R
import com.mabrouk.quran.domain.repository.SurahDefaultRepository
import com.mabrouk.quran.presentaion.utils.AUDIO_DOWNLOAD
import com.mabrouk.quran.presentaion.utils.AudioDataPass
import com.mabrouk.quran.presentaion.utils.LAST_ID
import com.mabrouk.quran.presentaion.utils.SURA_LIST_AUDIOS
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext


/**
 * @name Mohamed Mabrouk
 * Copyright (c) 4/17/22
 */
@HiltWorker
class AudioDownloader @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    val repository: SurahDefaultRepository,
    @IoDispatcher val io: CoroutineDispatcher
) : CoroutineWorker(context, params) {

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val channelId = "download_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Download Channel"
        val descriptionText = "Channel for download progress"
        val importance = NotificationManager.IMPORTANCE_LOW // Or appropriate importance
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }


    @SuppressLint("MissingPermission")
    private fun createForegroundInfo(downloadProgress: Int, videoTitle: String,content:String,total:Int) {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setContentTitle(videoTitle)
            .setContentText(content)
            .setTicker(videoTitle)
            .setProgress(total, downloadProgress, false)
            .setSmallIcon(com.mabrouk.core.R.drawable.ic_quran)
            .setOngoing(true)
            .build()

        notificationManager.notify(0,notification)
    }



    var result: String? = null
    override suspend fun doWork(): Result {
        val data = Data.Builder()

        inputData.getString(SURA_LIST_AUDIOS)?.let {
            fromToObject(it)?.apply {
                if (!FileUtils.fileIsFound(context, this.first().url, 1, 0)) {
                    downloadAya(this.first().url, 1, 0)
                }


                if (!FileUtils.fileIsFound(context, this.first().url, 1, 1)) {
                    downloadAya(this.first().url, 1, 1)
                }


                forEachIndexed { index,  item ->
                    result =
                        if (FileUtils.fileIsFound(context, item.url, item.chapterId, item.ayaNum)) {
                            null
                        } else {
                            createForegroundInfo(index+1,
                                context.getString(R.string.download_surah, item.surahName),
                                context.getString(
                                    R.string.aya_num, item.ayaNum
                                ),size)
                            downloadAya(item.url, item.chapterId, item.ayaNum)
                        }

                    if (repository.getSavedTafsir(item.chapterId, item.ayaNum).firstOrNull()
                            .isNullOrEmpty()
                    ) {
                        downloadTafsir(item.chapterId, item.ayaNum, 4)
                    }
                }
                data.putInt(LAST_ID, inputData.getInt(LAST_ID, 0)+1)
            }
        }
        data.putString(AUDIO_DOWNLOAD, result)
        if (result == null) {
            notificationManager.cancelAll()
            return Result.success(data.build())
        }
        return Result.failure(data.build())
    }

    suspend fun downloadAya(url: String, sura: Int, aya: Int): String? {
        var result: String? = null
        repository.downloadAudio("${BuildConfig.AUDIO_URL2}$url/${sura.decimalFormat()}${aya.decimalFormat()}.mp3")
            .collect {
                when (it) {
                    is OnSuccess -> {
                        Log.d(
                            "save File",
                            FileUtils.saveMp3(context, it.data, url, sura, aya).toString()
                        )
                    }

                    is OnFailure -> result = it.throwable.message!!
                    is NoInternetConnect -> result = it.error
                    is OnProgress ->{}
                    else -> {
                        Log.d("TAG", "")
                    }
                }
            }
        return result
    }

    private suspend fun downloadTafsir(chapterId: Int, verseId: Int, count: Int): String? {
        var result: String? = null
        repository.requestTafsir(chapterId, verseId, count).collect {
            when (it) {
                is OnSuccess -> {
                    withContext(io) {
                        repository.saveTafsir(it.data)
                    }
                    if (count > 1) {
                        downloadTafsir(chapterId, verseId, count - 1)
                    }
                }

                is OnFailure -> result = it.throwable.message!!
                is NoInternetConnect -> result = it.error
                else -> {
                    Log.d("TAG", "")
                }
            }
        }
        return result
    }

    private fun fromToObject(json: String?): ArrayList<AudioDataPass>? {
        val type = object : TypeToken<ArrayList<AudioDataPass>>() {}.type
        return Gson().fromJson(json, type)
    }

}