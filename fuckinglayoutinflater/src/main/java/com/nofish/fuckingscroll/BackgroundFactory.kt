package com.nofish.fuckingscroll

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import androidx.collection.ArrayMap
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class BackgroundFactory : LayoutInflater.Factory2 {
    private var mViewCreateFactory: LayoutInflater.Factory? = null
    private var mViewCreateFactory2: LayoutInflater.Factory2? = null
    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        //如果是要处理的View，代表已经进行了背景设置，无需再次创建，留给系统创建就行
        if (name.startsWith("com.fuck.harmonyos.view")) {
            return null
        }
        var view: View? = null

        //防止与其他调用factory库冲突，例如字体、皮肤替换库，用已经设置的factory来创建view

        if (mViewCreateFactory2 != null) {
            with(mViewCreateFactory2!!){
                view = this.onCreateView(name, context, attrs)
                if (view == null) {
                    view = mViewCreateFactory2!!.onCreateView(null, name, context, attrs)
                }
            }

        } else if (mViewCreateFactory != null) {
            view = mViewCreateFactory!!.onCreateView(name, context, attrs)
        }
        return setViewBackground(name, context, attrs, view)
    }

    fun setInterceptFactory(factory: LayoutInflater.Factory) {
        mViewCreateFactory = factory
    }

    fun setInterceptFactory2(factory: LayoutInflater.Factory2) {
        mViewCreateFactory2 = factory
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        return onCreateView(name, context, attrs)
    }

    companion object {
        private val sConstructorSignature = arrayOf(
            Context::class.java, AttributeSet::class.java
        )
        private val mConstructorArgs = arrayOfNulls<Any>(2)
        private val sConstructorMap: MutableMap<String, Constructor<out View>?> = ArrayMap()
        private val methodMap = HashMap<String, HashMap<String, Method>>()
        fun setViewBackground(context: Context, attrs: AttributeSet, view: View?): View? {
            return setViewBackground(null, context, attrs, view)
        }
        private fun setViewBackground(
            name: String?,
            context: Context,
            attrs: AttributeSet,
            view: View?
        ): View? {
            return null
        }

        private fun getMethod(clazz: Class<*>, methodName: String): Method? {
            var method: Method? = null
            var methodHashMap = methodMap[clazz.canonicalName.toString()]
            if (methodHashMap != null) {
                method = methodMap[clazz.canonicalName]!![methodName]
            } else {
                methodHashMap = HashMap()
                methodMap[clazz.canonicalName] = methodHashMap
            }
            if (method == null) {
                method = findMethod(clazz, methodName)
                if (method != null) {
                    methodHashMap[methodName] = method
                }
            }
            return method
        }

        private fun findMethod(clazz: Class<*>, methodName: String): Method? {
            val method: Method?
            method = try {
                clazz.getMethod(methodName)
            } catch (e: NoSuchMethodException) {
                findDeclaredMethod(clazz, methodName)
            }
            return method
        }

        private fun findDeclaredMethod(clazz: Class<*>, methodName: String): Method? {
            var method: Method? = null
            try {
                method = clazz.getDeclaredMethod(methodName)
                method.isAccessible = true
            } catch (e: NoSuchMethodException) {
                if (clazz.superclass != null) {
                    method = findDeclaredMethod(clazz.superclass, methodName)
                }
            }
            return method
        }

        private fun setDrawable(
            drawable: Drawable,
            view: View,
            otherTa: TypedArray,
            typedArray: TypedArray
        ) {
        }

        private fun setBackground(drawable: Drawable, view: View, typedArray: TypedArray) {}
        private fun createViewFromTag(context: Context, name: String, attrs: AttributeSet): View? {
            var name = name
            if (TextUtils.isEmpty(name)) {
                return null
            }
            if (name == "view") {
                name = attrs.getAttributeValue(null, "class")
            }
            return try {
                mConstructorArgs[0] = context
                mConstructorArgs[1] = attrs
                if (-1 == name.indexOf('.')) {
                    var view: View? = null
                    if ("View" == name) {
                        view = createView(context, name, "android.view.")
                    }
                    if (view == null) {
                        view = createView(context, name, "android.widget.")
                    }
                    if (view == null) {
                        view = createView(context, name, "android.webkit.")
                    }
                    view
                } else {
                    createView(context, name, null)
                }
            } catch (e: Exception) {
                Log.w("BackgroundLibrary", "cannot create 【$name】 : ")
                null
            } finally {
                mConstructorArgs[0] = null
                mConstructorArgs[1] = null
            }
        }

        @Throws(InflateException::class)
        private fun createView(context: Context, name: String, prefix: String?): View? {
            var constructor = sConstructorMap[name]
            return try {
                if (constructor == null) {
                    val clazz = context.classLoader.loadClass(
                        if (prefix != null) prefix + name else name
                    ).asSubclass(
                        View::class.java
                    )
                    constructor = clazz.getConstructor(*sConstructorSignature)
                    sConstructorMap[name] = constructor
                }
                constructor!!.isAccessible = true
                constructor.newInstance(*mConstructorArgs)
            } catch (e: Exception) {
                Log.w("BackgroundLibrary", "cannot create 【$name】 : ")
                null
            }
        }
    }
}