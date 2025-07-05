package com.mabrouk.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.JsonSyntaxException
import com.mabrouk.core.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import javax.net.ssl.SSLHandshakeException
import kotlin.collections.ArrayList

sealed class Result<out T : Any> {
    data object OnLoading : Result<Nothing>()
    data object OnFinish : Result<Nothing>()
    data class OnSuccess<out T : Any>(val data: T) : Result<T>()
    data class OnFailure(val throwable: Throwable) : Result<Nothing>()
    data class NoInternetConnect(val error: String) : Result<Nothing>()
    data class OnProgress<out T : Any>(val data: T) : Result<T>()
}

typealias ApiResult<T> = suspend () -> Response<T>
typealias ApiResult2<T> = suspend () -> Deferred<T>

fun <T : Any> executeCall(
    context: Context,
    messageInCaseOfError: String = "Network error",
    allowRetries: Boolean = true,
    numberOfRetries: Int = 2,
    apiCall: ApiResult<T>
): Flow<Result<T>> {
    return flow {
        emit(Result.OnLoading)
        val response = apiCall()
        if (response.body() is ResponseBody){
            response.body()?.let {
                emit(Result.OnProgress(it))
            }
        }

        if (response.isSuccessful) {
            response.body()?.let {
                emit(Result.OnSuccess(it))
            } ?: emit(Result.OnFailure(IOException("API call successful but empty response body")))
            return@flow
        }

        emit(
            Result.OnFailure(
                IOException(
                    "API call failed with error - ${
                        response.errorBody()?.string() ?: messageInCaseOfError
                    }"
                )
            )
        )
        return@flow
    }.final(context, allowRetries, numberOfRetries)
}


fun <T : Any> executeCall2(
    context: Context,
    allowRetries: Boolean = true,
    numberOfRetries: Int = 2,
    apiCall: ApiResult2<T>
): Flow<Result<T>> {
    return flow {
        emit(Result.OnLoading)
        val response = apiCall()
        emit(Result.OnSuccess(response.await()))
        return@flow
    }.final(context, allowRetries, numberOfRetries)
}

fun <T : Any> Flow<Result<T>>.final(
    context: Context,
    allowRetries: Boolean = true,
    numberOfRetries: Int = 2,
) : Flow<Result<T>>{
    var delayDuration = 1000L
    val delayFactor = 2
   return this.catch { e ->
        if (CheckNetwork.isOnline(context)) {
            emit(Result.OnFailure(IOException(errorMsg(e, context))))
        } else {
            emit(Result.NoInternetConnect(context.getString(R.string.no_internet_connection)))
        }
        return@catch
    }.retryWhen { cause, attempt ->
        if (!allowRetries || attempt > numberOfRetries || cause !is IOException) return@retryWhen false
        delay(delayDuration)
        delayDuration *= delayFactor
        return@retryWhen true
    }.onCompletion {
        emit(Result.OnFinish)
    }.flowOn(Dispatchers.IO)
}



interface OnCheckConnection {
    fun ConnectionTrue()
    fun ConnectionError()
}

class CheckNetwork {
    companion object {

        fun isConnected(context: Context, onCheckConnection: OnCheckConnection) {
            if (isOnline(context)) {
                onCheckConnection.ConnectionTrue()
            } else {
                onCheckConnection.ConnectionError()
            }
        }

        @SuppressLint("NewApi")
        fun isOnline(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                        return true
                    }
                }
            }
            return false
        }
    }
}

fun errorMsg(e: Throwable, context: Context): String {
    Log.d("errorMsg",e.message.toString())
   return when (e) {
        is HttpException -> analysisError(e)
        is SocketTimeoutException -> context.getString(R.string.socketTimeout)
        is JsonSyntaxException -> context.getString(R.string.Jsonpars)
        is SSLHandshakeException -> e.message.toString()
        else -> "Api Error"
    }
}



private fun analysisError(e: HttpException): String {
    return try {
        val responseStrings: String = e.response()!!.errorBody().toString()
        val jsonObject = JSONObject(responseStrings)
        when {
            jsonObject.has("msg") -> jsonObject.get("msg").toString()
            jsonObject.has("error") -> jsonObject.get("error").toString()
            else -> e.message()
        }
    } catch (ex: Exception) {
        (if (e.message().isEmpty()) e.localizedMessage else e.message()) as String
    }
}

fun <T : Any> List<T>.toArrayList() = ArrayList(this)

fun Int.decimalFormat(): String {
    val pattern = "000"
    val number = NumberFormat.getNumberInstance(Locale.US)
    val myFormatter = number as DecimalFormat
    myFormatter.applyPattern(pattern)
    return myFormatter.format(this)
}
