package com.nofish.imageframe

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.load
import coil.request.CachePolicy
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.size.Scale
import coil.transform.CircleCropTransformation
import coil.util.CoilUtils
import com.bumptech.glide.Glide
import com.nofish.imageframe.databinding.ActivityMainBinding
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        val request = ImageRequest.Builder(this@MainActivity)
//            .data("https://zyc-essay.oss-cn-beijing.aliyuncs.com/share/58299381-0aea-46c9-bbfb-28921d8e7db6.jpeg")
//            .size(400, 300) // 设置图像的目标尺寸
//            .placeholder(R.mipmap.placeholder) // 设置占位图
//            .error(R.mipmap.error) // 设置加载错误时显示的图像
//            .build()
        val request = ImageRequest.Builder(this@MainActivity)
            .data("https://zyc-essay.oss-cn-beijing.aliyuncs.com/share/58299381-0aea-46c9-bbfb-28921d8e7db6.jpeg")
            .size(400, 300) // 设置图像的目标尺寸
            .scale(Scale.FIT)//Scale.FILL / Scale.FIT
            .networkCachePolicy(CachePolicy.ENABLED).error(R.mipmap.error) // 设置加载错误时显示的图像
            .placeholder(R.mipmap.placeholder) // 设置占位图
            .crossfade(true).transformations(CircleCropTransformation())
            .memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED)
            .listener(
                onStart = {/* 加载开始时执行的操作 */ },
                onCancel = {/* 加载取消时执行的操作 */ },
                onError = { request, result ->/* 加载失败时执行的操作 */ },
                onSuccess = { request, result ->/* 加载成功时执行的操作 */ }
            ).build()

        val a = ImageLoader.Builder(this@MainActivity)
            .crossfade(true)
            .build()


        binding.run {
            iv1.load("https://zyc-essay.oss-cn-beijing.aliyuncs.com/share/58299381-0aea-46c9-bbfb-28921d8e7db6.jpeg")
            val disposable = iv2.load(request)
            disposable.dispose()

            iv1.load("",)
        }


    }
}
class MyApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .crossfade(true)
//            .diskCache(CoilUtils.createDefaultCache(applicationContext))
//            .okHttpClient {
//                OkHttpClient.Builder()
//                    .cache(CoilUtils.createDefaultCache(applicationContext))
//                    .build()
//            }
            .build()
    }
}