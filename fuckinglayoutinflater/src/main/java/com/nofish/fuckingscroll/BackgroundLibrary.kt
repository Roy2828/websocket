package com.nofish.fuckingscroll

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat

object BackgroundLibrary {
    fun inject(context: Context?): LayoutInflater? {
        val inflater: LayoutInflater? = if (context is Activity) {
            context.layoutInflater
        } else {
            LayoutInflater.from(context)
        }
        if (inflater == null) {
            return null
        }
        if (inflater.factory2 == null) {
            val factory = setDelegateFactory(context!!)
            inflater.factory2 = factory
        } else if (inflater.factory2 !is BackgroundFactory) {
            forceSetFactory2(inflater)
        }
        return inflater
    }
    /**
     * 注入自定义 LayoutInflater 工厂的主方法
     * 如果因为其他库已经设置了factory，可以使用该方法去进行inject，在其他库的setFactory后面调用即可
     */
    fun inject2(context: Context?): LayoutInflater? {
        // 根据 Context 类型获取 LayoutInflater 实例
        val inflater: LayoutInflater? = if (context is Activity) {
            context.layoutInflater
        } else {
            LayoutInflater.from(context)
        }
        if (inflater == null) {
            return null
        }
        // 强制设置自定义工厂
        forceSetFactory2(inflater)
        return inflater
    }

    // 创建并配置 BackgroundFactory 实例
    private fun setDelegateFactory(context: Context): BackgroundFactory {
        val factory = BackgroundFactory()
        if (context is AppCompatActivity) {
            // 如果是 AppCompatActivity 实例，使用其委托创建视图
            val delegate = context.delegate
            factory.setInterceptFactory { name, context, attrs ->
                delegate.createView(null, name, context, attrs)
            }
        }
        return factory
    }

    // 通过反射技术强制为 LayoutInflater 设置自定义工厂
    @SuppressLint("DiscouragedPrivateApi")
    private fun forceSetFactory2(inflater: LayoutInflater) {
        val compatClass = LayoutInflaterCompat::class.java
        val inflaterClass = LayoutInflater::class.java
        try {
            // 访问私有字段并修改其值，以便可以设置自定义工厂
            val sCheckedField = compatClass.getDeclaredField("sCheckedField").apply {
                isAccessible = true
                setBoolean(compatClass, false)
            }
            val mFactory = inflaterClass.getDeclaredField("mFactory").apply {
                isAccessible = true
            }
            val mFactory2 = inflaterClass.getDeclaredField("mFactory2").apply {
                isAccessible = true
            }
            // 创建 BackgroundFactory 实例
            val factory = BackgroundFactory()
            if (inflater.factory2 != null) {
                factory.setInterceptFactory2(inflater.factory2)
            } else if (inflater.factory != null) {
                factory.setInterceptFactory(inflater.factory)
            }
            // 设置工厂到 LayoutInflater 的 mFactory 和 mFactory2 字段
            mFactory2[inflater] = factory
            mFactory[inflater] = factory
        } catch (e: IllegalAccessException) {
            // 处理反射访问异常
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            // 处理反射访问异常
            e.printStackTrace()
        }
    }
}
