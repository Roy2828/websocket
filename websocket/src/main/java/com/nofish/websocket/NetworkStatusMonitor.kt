package com.nofish.websocket

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log

/**
 * 小心内存泄漏
 */
object NetworkStatusMonitor {
    private val connectivityService by lazy {
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private var isRegistered = false;
    var isAvailable = false
        private set
    var isUnAvailable = false
        private set
        get() {
            return !isAvailable
        }
    private val mCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // 联网后开始重连
                isAvailable = true
                Log.e("", "onAvailable")
            }

            override fun onLost(network: Network) {
                // 断网停止重连
                isAvailable = false
                Log.e("", "onLost")

            }
        }
    }

    fun register(callback: ConnectivityManager.NetworkCallback) {
        connectivityService.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(), callback
        )
        if (!isRegistered) {
            isRegistered = true
            register(callback = mCallback)
        }
    }


    fun unregister(callback: ConnectivityManager.NetworkCallback) {
        connectivityService.unregisterNetworkCallback(callback)
    }

    /**
     * 判断网络是否连接
     */
    fun isNetworkConnected(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val mNetworkInfo = connectivityService.activeNetworkInfo
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable
            }
        } else {
            val network = connectivityService.activeNetwork ?: return false
            val status = connectivityService.getNetworkCapabilities(network)
                ?: return false
            if (status.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                return true
            }
        }
        return false
    }

    /**
     * 判断是否是WiFi连接
     */
    fun isWifiConnected(context: Context?): Boolean {
        if (context != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                val mWiFiNetworkInfo = connectivityService
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                if (mWiFiNetworkInfo != null) {
                    return mWiFiNetworkInfo.isAvailable
                }
            } else {
                val network = connectivityService.activeNetwork ?: return false
                val status = connectivityService.getNetworkCapabilities(network)
                    ?: return false
                if (status.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 判断是否是数据网络连接
     */
    fun isMobileConnected(context: Context?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val mMobileNetworkInfo = connectivityService
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            if (mMobileNetworkInfo != null) {
                return mMobileNetworkInfo.isAvailable
            }
        } else {
            val network = connectivityService.activeNetwork ?: return false
            val status = connectivityService.getNetworkCapabilities(network)
                ?: return false
            status.transportInfo
            if (status.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            }
        }
        return false
    }

}

