package com.mabrouk.quran.presentaion.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.mabrouk.core.utils.DataStorePreferences
import com.mabrouk.core.utils.FileUtils
import com.mabrouk.core.utils.FileUtils.getMp3Path
import com.mabrouk.quran.domain.models.QuranReader
import com.mabrouk.quran.domain.models.Surah
import com.mabrouk.quran.domain.models.Verse
import com.mabrouk.quran.domain.repository.QuranDefaultRepository
import com.mabrouk.quran.domain.repository.SurahDefaultRepository
import com.mabrouk.quran.presentaion.states.SurahStates
import com.mabrouk.quran.presentaion.states.VersesDownloadStates
import com.mabrouk.quran.presentaion.utils.AudioDataPass
import com.mabrouk.quran.presentaion.utils.LAST_ID
import com.mabrouk.quran.presentaion.utils.READER_KEY
import com.mabrouk.quran.presentaion.utils.SURA_LIST_AUDIOS
import com.mabrouk.quran.presentaion.workers.AudioDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.mockito.internal.matchers.Null
import javax.inject.Inject
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata

@HiltViewModel
class SurahViewModel @Inject constructor(
    val repository: SurahDefaultRepository,
    private val dataStore: DataStorePreferences,
    val factory :ListenableFuture<MediaController>,
    @ApplicationContext val context: Context
) : ViewModel() {
    private val workManger by lazy {  WorkManager.getInstance(context) }
    private val _states = MutableStateFlow<VersesDownloadStates>(VersesDownloadStates.IDLE)
    val states= _states.asStateFlow()
    private val _versesList = MutableStateFlow<List<Verse>>(emptyList())
    val versesList = _versesList.asStateFlow()
     private val _reader = MutableStateFlow<List<QuranReader>>(emptyList())
    val reader = _reader.asStateFlow()
    private val _currentReader = MutableStateFlow<QuranReader?>(null)
    val currentReader = _currentReader.asStateFlow()
    lateinit var surahname: String
    private val _currentAyaIndex = MutableStateFlow(-1)
    val currentAyaIndex = _currentAyaIndex.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private val listener = object : Player.Listener{
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
                setCurrentAyaIndex(mediaItem?.mediaId?.toInt() ?: -1)
        }
    }
    var player : Player?= null

    init {
        viewModelScope.launch {
            getCurrentReader()
        }
        initPlayer()
    }


    private fun initPlayer(){
        if (player==null){
            factory.addListener({
                player = factory.get()
                player?.apply {
                    addListener(listener)
                    repeatMode = Player.REPEAT_MODE_OFF
                }
            }, MoreExecutors.directExecutor())
        }
    }

    fun setCurrentAyaIndex(index: Int){
        _currentAyaIndex.value = index
    }

    fun downloadVerseAudio(
        surahname: String,
        verses: List<Verse>,
        allVerses: List<List<Verse>>,
        index: Int,
        id: Int
    ) {
        this.surahname = surahname
        val map = verses.map {
            AudioDataPass(
                it.id,
                currentReader.value?.sufix?:"",
                it.chapterId,
                it.verseNumber,
                surahname
            )
        }
        val data = Data.Builder().putString(SURA_LIST_AUDIOS, Gson().toJson(ArrayList(map))).putInt(
            LAST_ID,index).build()
        val request = OneTimeWorkRequest.Builder(AudioDownloader::class.java)
            .setInputData(data)
            .build()
        workManger.enqueue(request)
        viewModelScope.launch {
            workManger.getWorkInfoByIdFlow(request.id).collectLatest {
               // _states.value = it
                when(it?.state){
                    WorkInfo.State.RUNNING -> {

                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val nextIndex = it.outputData.getInt(LAST_ID, -1)
                            setupMediaItems(context,allVerses[nextIndex-1])
                        if (nextIndex < verses.size){
                            if (nextIndex == 1 && player?.isPlaying==true){
                                _currentAyaIndex.value = 0
                            }
                            downloadVerseAudio(surahname, allVerses[nextIndex],allVerses, nextIndex,id)
                        }else{
                             updateSurah(id)
                                updateReader(id.toLong())
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }

    fun updateSurah(id: Int) = viewModelScope.launch {
        repository.updateSurahAudioDownload(id,true)
    }

    fun updateReader(suraId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val reader = currentReader.value?.let {
                if (it.versesIds?.contains(suraId)==false){
                    workManger.cancelAllWork()
                }
                if (it.versesIds.isNullOrEmpty()) {
                    it.versesIds = arrayListOf(suraId)
                }else{
                    it.versesIds = it.versesIds?.toHashSet()?.apply {
                        add(suraId)
                    }?.let { it1 -> ArrayList(it1) }
                }
                it
            }
            reader?.let {
                repository.updateQuranReader(it)
                dataStore.setData(READER_KEY,it)
            }
        }
    }

    fun loadVerses(surahId: Int) {
        viewModelScope.launch {
            repository.getSavedVerses(surahId).collectLatest {
                _versesList.value = it
            }
        }
    }

    fun getAllReader(){
        viewModelScope.launch {
            repository.getReaders().collectLatest {
                _reader.value = it.let {
                    it.find { currentReader.value?.readerId == it.readerId }?.isSelected = true
                    it
                }
            }
        }

    }

    fun setupMediaItems(context: Context,list: List<Verse>){
        player?.addMediaItems(list.map {
            addMediaItem(
                it
            )
        })
        player?.prepare()
    }

    private fun addMediaItem(verse:Verse): MediaItem {
        return MediaItem.Builder().setUri(
            getMp3Path(
                context,
                currentReader.value?.sufix ?: "",
                verse.chapterId,
                verse.verseNumber
            ).toUri()
        ).setMediaId("${verse.verseNumber - 1}")
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(verse.textIndopak)
                .build())
            .build()
    }

    fun updateReader(context: Context,reader: QuranReader){
        _currentReader.value = reader
        viewModelScope.launch {
           // downloadVerseAudio(context,surahname,versesList.value.take(7),0)
            val item = versesList.value.firstOrNull()
            if (FileUtils.fileIsFound(context, currentReader.value?.sufix?:"", item?.chapterId?:1, item?.verseNumber?:1)){
                player?.clearMediaItems()
                setupMediaItems(context, versesList.value)
            }
            dataStore.setData(READER_KEY,currentReader)
        }
    }

    private suspend fun getCurrentReader() {
        _currentReader.value = dataStore.getData<QuranReader>(READER_KEY).firstOrNull() ?: QuranReader(
            1,
            "عبد الباسط",
            "abdulbasit_abdulsamad_mujawwad/128"
        )
    }

    override fun onCleared() {
        super.onCleared()
        player?.removeListener(listener)
    }
}