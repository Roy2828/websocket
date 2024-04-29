package com.nofish.plugins.click

import java.io.Serializable

// 定义一个数据类 com.nofish.plugins.click.ClickConfig，用于表示视图点击的配置。
internal data class ClickConfig(
    val clickCheckClass: String,  // 点击检查类的完整名称
    val onClickMethodName: String,  // onClick方法的名字
    val checkViewOnClickAnnotation: String,  // 检查ViewOnClick注解的描述符
    val uncheckViewOnClickAnnotation: String,  // 未检查ViewOnClick注解的描述符
    val hookPointList: List<ClickHookPoint>,  // 插桩的点列表
    val include: List<String>  // 包含的类或方法列表
) : Serializable {  // 实现Serializable接口，可序列化

    val onClickMethodDesc = "(Landroid/view/View;)Z"  // onClick方法的描述符

    companion object {
        // 使用 com.nofish.plugins.click.ClickPluginParameter 创建 com.nofish.plugins.click.ClickConfig 的实例
        operator fun invoke(pluginParameter: ClickPluginParameter): ClickConfig {
            return ClickConfig(
                clickCheckClass = pluginParameter.clickCheckClass.replace(
                    ".",
                    "/"
                ), // 将'.'替换为'/'，这样的格式是用于字节码的
                onClickMethodName = pluginParameter.onClickMethodName,
                checkViewOnClickAnnotation = formatAnnotationDesc(desc = pluginParameter.checkAndroidOnClickAnnotation),
                uncheckViewOnClickAnnotation = formatAnnotationDesc(desc = pluginParameter.uncheckViewOnClickAnnotation),
                include = pluginParameter.include,
                hookPointList = listOf(
                    ClickHookPoint(
                        interfaceName = "android/view/View\$OnClickListener",  // onClick监听器的接口名称
                        methodName = "onClick",  // 方法名称
                        nameWithDesc = "onClick(Landroid/view/View;)V"  // 方法的名称及其描述符
                    ),
                )
            )
        }

        // 将注解描述的格式转换为字节码格式
        private fun formatAnnotationDesc(desc: String): String {
            return "L" + desc.replace(".", "/") + ";"  // 使用 'L' 开头，并将'.'替换为'/'，最后使用';'结束，这是字节码的格式
        }
    }
}

// 定义一个数据类 com.nofish.plugins.click.ClickHookPoint，表示插桩的点。
internal data class ClickHookPoint(
    val interfaceName: String,  // 接口名称
    val methodName: String,  // 方法名称
    val nameWithDesc: String,  // 方法的名称及其描述符
) : Serializable {  // 实现Serializable接口，可序列化

    val interfaceSignSuffix = "L$interfaceName;"  // 获取接口的签名后缀
}

// 定义一个开放类 com.nofish.plugins.click.ClickPluginParameter，用于存储插桩参数。
open class ClickPluginParameter {
    var clickCheckClass = ""  // 点击检查类的名称
    var onClickMethodName = "onClick"  // onClick方法的名称
    var checkAndroidOnClickAnnotation = ""  // 检查android:onClick注解的描述
    var uncheckViewOnClickAnnotation = ""  // 未检查ViewOnClick注解的描述
    var include = listOf<String>()  // 包含的类或方法列表
    override fun toString(): String {
        return "ClickPluginParameter(clickCheckClass='$clickCheckClass', onClickMethodName='$onClickMethodName', checkAndroidOnClickAnnotation='$checkAndroidOnClickAnnotation', uncheckViewOnClickAnnotation='$uncheckViewOnClickAnnotation', include=$include)"
    }

}