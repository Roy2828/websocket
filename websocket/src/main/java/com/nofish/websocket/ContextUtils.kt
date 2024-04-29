package com.nofish.websocket

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri

/**
 * @Author AlbertZyc
 * @Date 2022/5/6
 * @description 提供context的工具类
 */
val appContext: Context by lazy {
    ContextUtils.context
}

@SuppressLint("StaticFieldLeak")
class ContextUtils : ContentProvider() {

    companion object {
        //  Get Context at anywhere
        lateinit var context: Context

        lateinit var resource: Resources

        private fun setContextInstance(context: Context) {
            Companion.context = context
            resource = Companion.context.resources
        }

    }

    override fun onCreate(): Boolean {
        setContextInstance(context!!)
        return true
    }

    override fun query(
        uri: Uri, strings: Array<String>?, s: String?, strings1: Array<String>?,
        s1: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        contentValues: ContentValues?,
        s: String?,
        strings: Array<String>?
    ): Int {
        return 0
    }
}