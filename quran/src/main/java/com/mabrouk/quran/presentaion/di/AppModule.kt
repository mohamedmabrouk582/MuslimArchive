package com.mabrouk.quran.presentaion.di


import com.mabrouk.quran.data.repository.SurahRepository
import com.mabrouk.quran.data.repository.QuranRepository
import com.mabrouk.quran.domain.repository.SurahDefaultRepository
import com.mabrouk.quran.domain.repository.QuranDefaultRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @name Mohamed Mabrouk
 * Copyright (c) 4/16/22
 */
@Module
@InstallIn(SingletonComponent::class)
 abstract class AppModule {

    @Binds
    @Singleton
    abstract fun getQuranRepository(repository: QuranRepository): QuranDefaultRepository

    @Binds
    @Singleton
    abstract fun getAyaRepository(repository: SurahRepository): SurahDefaultRepository
}