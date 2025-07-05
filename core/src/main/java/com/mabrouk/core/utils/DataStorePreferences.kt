package com.mabrouk.core.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStorePreferences @Inject constructor(@ApplicationContext val context: Context) {

    companion object{
       const val  FILE_NAME = "settings"
    }

    val Context.dataStore by preferencesDataStore(name = FILE_NAME)

    suspend fun setString(key: String,value: String){
        context.dataStore.edit {
            it[stringPreferencesKey(key)] = value
        }
    }

    suspend fun setLong(key: String,value: Long){
        context.dataStore.edit {
            it[longPreferencesKey(key)] = value
        }
    }

    suspend fun getString(key: String) : String{
      return  context.dataStore.data.map {
            it[stringPreferencesKey(key)] ?: ""
        }.first()
    }

    suspend fun getLong(key: String) : Long{
        return  context.dataStore.data.map {
            it[longPreferencesKey(key)] ?: 0L
        }.first()
    }

    inline fun <reified  t:Any> getData(key:String) : Flow<t> {
       return context.dataStore.data.map {
           val type=object : TypeToken<t>(){}.type
           Gson().fromJson(it[stringPreferencesKey(key)]?:"",type)
        }
    }

    suspend inline fun <reified  t:Any> setData(key:String, value:t) {
        context.dataStore.edit {
            it[stringPreferencesKey(key)]=Gson().toJson(value)
        }
    }

    suspend fun getBoolean(key:String) : Boolean{
        return context.dataStore.data.map {
            it[booleanPreferencesKey(key)]?:false
        }.first()
    }

    suspend fun setBoolean(key:String,value:Boolean){
        context.dataStore.edit {
           it[booleanPreferencesKey(key)]=value
        }
    }

}