package com.nofish.plugins.click

// 导入相关的Android Gradle API和其他依赖
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

class ClickPlugin : Plugin<Project> {

    // 插件的应用方法，当应用插件时会被调用
    override fun apply(project: Project) {
        println("ClickPlugin apply")
        // 创建一个扩展，该扩展允许用户在 build.gradle 中配置插件参数
        project.extensions.create<ClickPluginParameter>(name = ClickPluginParameter::class.java.simpleName)

        // 获取Android插件的 AndroidComponentsExtension，这是AGP 7.0及更高版本中用于配置构建变体的API
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

        // 对每一个构建变体进行操作
        androidComponents.onVariants { variant ->

            // 获取配置的插件参数
            val pluginParameter = project.extensions.getByType<ClickPluginParameter>()
            println("ClickPlugin pluginParameter = $pluginParameter")

            // 使用 ClickClassVisitorFactory 来转换类
            // InstrumentationScope.ALL 表示所有的类都会被转换
            variant.instrumentation.transformClassesWith(
                ClickClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) { params ->
                // 为转换设置配置参数
                params.config.set(ClickConfig(pluginParameter = pluginParameter))
            }

            // 设置ASM框架计算模式为 COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            // 这意味着只有被插桩的方法才会计算框架，这可以提高性能
            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            )
        }
    }
}
