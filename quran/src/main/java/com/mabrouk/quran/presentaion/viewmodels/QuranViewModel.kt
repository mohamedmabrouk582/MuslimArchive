package com.mabrouk.quran.presentaion.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.reflect.TypeToken
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.mabrouk.core.network.Result
import com.mabrouk.core.utils.DataStorePreferences
import com.mabrouk.quran.domain.models.Juz
import com.mabrouk.quran.domain.models.JuzSurah
import com.mabrouk.quran.domain.models.QuranReader
import com.mabrouk.quran.domain.models.Surah
import com.mabrouk.quran.domain.models.SurahFts
import com.mabrouk.quran.domain.models.Verse
import com.mabrouk.quran.domain.repository.QuranDefaultRepository
import com.mabrouk.quran.presentaion.states.DownloadStates
import com.mabrouk.quran.presentaion.states.QuranStates
import com.mabrouk.quran.presentaion.utils.READERS_DOWNLOADS
import com.mabrouk.quran.presentaion.utils.SURAH_LIST_DOWNLOADS
import com.mabrouk.quran.presentaion.utils.VERSES_IDS
import com.mabrouk.quran.presentaion.utils.mapJuz
import com.mabrouk.quran.presentaion.workers.SurahDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QuranViewModel @Inject constructor(
    val repository: QuranDefaultRepository,
    private val remoteConfig: FirebaseRemoteConfig,
    private val dataStore: DataStorePreferences ,
    @ApplicationContext val context: Context
) : ViewModel() {
    private val _loading = MutableStateFlow<DownloadStates>(DownloadStates.Idle)
    val loading = _loading.asStateFlow()
    private var _quranStates = MutableStateFlow<QuranStates>(QuranStates.IDLE)
    val quranStates: StateFlow<QuranStates> = _quranStates
    var searchJob: Job? = null
    private var juzs: List<Juz> = emptyList()
    private var surahs: List<Surah> = emptyList()
    private val instance by lazy {  WorkManager.getInstance(context) }



    fun loadData() {
        viewModelScope.launch {
            if (repository.isSurahTableEmpty()) {
                requestJuz()
                requestSurahs()
            } else {
                withContext(Dispatchers.IO) {
                    repository.getJuzSurahs().let {
                        _quranStates.value = QuranStates.LoadJuzSurahs(ArrayList(it))
                    }
                }
            }
        }
         getReaders()
    }

    fun startSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                _quranStates.value = QuranStates.ClearSearch
            } else executeSearch(query)
        }
    }

    fun updateVerseDownload(isDownload: Boolean,id: Int){
        viewModelScope.launch {
            repository.updateVerseDownload(isDownload,id)
        }
    }

    private fun requestJuz() {
        viewModelScope.launch {
            repository.requestJuz().collect {
                when (it) {
                    is Result.OnFinish -> {

                    }

                    is Result.NoInternetConnect -> {
                        _quranStates.value = QuranStates.Error(it.error)
                    }

                    is Result.OnFailure -> {
                        Log.d("TAG", it.throwable.message.toString())
                        _quranStates.value = QuranStates.Error(it.throwable.message.toString())
                    }

                    is Result.OnLoading -> {
                        _quranStates.value = QuranStates.Loading
                    }

                    is Result.OnSuccess -> {
                        juzs = it.data.juzs.distinctBy { it.juzNumber }
                        if (surahs.isNotEmpty()) {
                            _quranStates.value = QuranStates.LoadJuzSurahs(mapJuz(juzs, surahs))
                        }
                        withContext(Dispatchers.IO){
                            repository.saveJuz(it.data.juzs)
                        }
                    }

                    is Result.OnProgress ->{}
                }
            }
        }
    }

   private fun requestSurahs() {
        viewModelScope.launch {
            repository.requestSurahs().collect {
                when (it) {
                    is Result.OnFinish -> {

                    }

                    is Result.NoInternetConnect -> {
                        _quranStates.value = QuranStates.Error(it.error)
                    }

                    is Result.OnFailure -> {
                        _quranStates.value = QuranStates.Error(it.throwable.message.toString())
                    }

                    is Result.OnLoading -> {
                        _quranStates.value = QuranStates.Loading
                    }

                    is Result.OnSuccess -> {
                        surahs = it.data.chapters
                        if (juzs.isNotEmpty()) {
                            _quranStates.value = QuranStates.LoadJuzSurahs(mapJuz(juzs, surahs))
                        }
                            repository.saveSurahs(it.data.chapters)
                    }

                    is Result.OnProgress -> {}
                }
            }
        }
    }

    suspend fun executeSearch(query: String) {
        repository.searchBySurah(query).first().let {
            mapSearchData(it)
        }
    }

    private fun mapSearchData(items: List<SurahFts>) {
        if (items.isEmpty()) return
        val data: ArrayList<JuzSurah> = ArrayList()
        juzs.forEach {
            val ids = arrayListOf<Int>()
            val surahs: ArrayList<JuzSurah> = ArrayList()
            it.verseMapping?.keys?.forEach { key ->
                items.find { it.id == key.toInt() }?.apply {
                    ids.add(this.id)
                    surahs.add(
                        JuzSurah(
                            it.juzNumber,
                            arrayListOf(),
                            it.verseMapping,
                            Surah(
                                id = this.id,
                                nameArabic = this.nameArabic,
                                nameComplex = this.nameComplex,
                                nameSimple = this.nameSimple,
                                isDownload = this.isDownload
                            ),
                            it.verseMapping[key],
                            it.isDownload
                        )
                    )
                }
            }

            if (ids.isNotEmpty()) data.add(JuzSurah(
                it.juzNumber,
                it.verseMapping?.keys?.map { it.toInt() } ?: arrayListOf(),
                it.verseMapping,
                null,
                null,
                true
            ))
            data.addAll(surahs)
        }
        _quranStates.value = QuranStates.SearchResult(
            data
        )
    }

    fun surahListDownloads(bool: Boolean = true) {
        viewModelScope.launch { dataStore.setBoolean(SURAH_LIST_DOWNLOADS, bool) }
    }

    fun downloadSurahs(ids: List<Int>) {
        viewModelScope.launch {
            val putIntArray =
                Data.Builder().putIntArray(VERSES_IDS, ids.map { it }.toIntArray())
            val build = OneTimeWorkRequest.Builder(SurahDownloadWorker::class.java)
                .setInputData(putIntArray.build())
                .build()

            instance.enqueue(build)
            instance.getWorkInfoByIdFlow(build.id).collectLatest {
                when (it?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _loading.value = DownloadStates.Success(ids)
                    }

                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
                        _loading.value = DownloadStates.Loading
                    }

                    WorkInfo.State.BLOCKED -> {

                    }

                    WorkInfo.State.CANCELLED, WorkInfo.State.FAILED, null -> {
                        _loading.value = DownloadStates.Error
                    }
                }
            }
        }
    }

    suspend fun updateJuz(juz: Juz) {
        repository.updateJuz(juz)
    }

    private fun getReaders() {
        viewModelScope.launch {
            if (!dataStore.getBoolean(READERS_DOWNLOADS)) {
                remoteConfig.fetchAndActivate()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val type = object : TypeToken<ArrayList<QuranReader>>() {}.type
                            val tt = remoteConfig.getString("readers")
                            val data: ArrayList<QuranReader> = Gson().fromJson(
                                tt, type
                            )
                            viewModelScope.launch(Dispatchers.IO) {
                                repository.insertReaders(data)
                                dataStore.setBoolean(READERS_DOWNLOADS, true)
                            }
                        }

                    }
            }
        }
    }

}