package com.nofish.plugins.click

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.nofish.plugins.filterLambda
import com.nofish.plugins.hasAnnotation
import com.nofish.plugins.isStatic
import com.nofish.plugins.nameWithDesc
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

internal interface ClickConfigParameters : InstrumentationParameters {
    /**
     * @get:Input:告诉 Gradle 这个属性是任务的输入
     * 意味着当这个属性的值变化时，相关的任务可能需要重新执行。
     * 在这里告诉构建系统当 config 属性发生变化时，需要重新进行相关的插桩操作。
     */
    @get:Input
    val config: Property<ClickConfig>
}


internal abstract class ClickClassVisitorFactory :
    AsmClassVisitorFactory<ClickConfigParameters> {

    // 创建一个 ClassVisitor
    // 如果当前类的名称与配置中的“include”条目匹配，将返回 ViewClickClassVisitor
    // 否则，将返回下一个 ClassVisitor
    override fun createClassVisitor(
        classContext: ClassContext,   // 当前正在处理的类的上下文信息
        nextClassVisitor: ClassVisitor  // 链中的下一个 ClassVisitor
    ): ClassVisitor {
//        println("ClickPlugin createClassVisitor = ${classContext.currentClassData.className}")
        // 从参数中获取 ViewClickConfig 配置
        val config = parameters.get().config.get()

        // 获取配置中的“include”条目
        val include = config.include

        // 获取当前正在处理的类的名称
        val className = classContext.currentClassData.className

        // 检查 className 是否以配置中的任何前缀开始
        if (include.any {
                className.startsWith(prefix = it)
            }
        ) {
            println("ClickPlugin createClassVisitor = $className")

            // 如果是，返回一个新的 ClickClassVisitor 实例
            return ClickClassVisitor(
                nextClassVisitor = nextClassVisitor,
                config = config
            )
        }

        // 否则，直接返回下一个 ClassVisitor
        return nextClassVisitor
    }
    override fun isInstrumentable(classData: ClassData): Boolean {
        return true
    }
}


private class ClickClassVisitor(
    private val nextClassVisitor: ClassVisitor,
    private val config: ClickConfig,
) : ClassNode(Opcodes.ASM5) {

    private companion object {

        private const val ViewDescriptor = "Landroid/view/View;"

        private const val ButterKnifeOnClickAnnotationDesc = "Lbutterknife/OnClick;"

    }
    override fun visitEnd() {
        super.visitEnd()

        // 用于收集需要hook的方法的集合
        val shouldHookMethodList = mutableSetOf<MethodNode>()

        // 遍历当前类的所有方法
        methods.forEach { methodNode ->
            when {
                methodNode.isStatic -> {
                    //不处理静态方法
                }

                methodNode.hasUncheckViewOnClickAnnotation() -> {
                    //不处理包含 UncheckViewOnClick 注解的方法
                    println("ClickPlugin visitEnd hasUncheckViewOnClickAnnotation")

                }

                methodNode.hasCheckViewAnnotation() -> {
                    //使用了 CheckViewOnClick 注解的情况，加入到hook列表中
                    shouldHookMethodList.add(methodNode)
                    println("ClickPlugin visitEnd hasCheckViewAnnotation")

                }

                methodNode.hasButterKnifeOnClickAnnotation() -> {
                    //使用了 ButterKnife OnClick 注解的情况，加入到hook列表中
                    shouldHookMethodList.add(methodNode)
                    println("ClickPlugin visitEnd hasButterKnifeOnClickAnnotation")
                }

                methodNode.isHookPoint() -> {
                    //是一个指定的hook点，比如匿名内部类的情况，加入到hook列表中
                    shouldHookMethodList.add(methodNode)
                    println("ClickPlugin visitEnd isHookPoint")
                }
            }

            //判断方法内部是否有需要处理的 lambda 表达式
            val dynamicInsnNodes = methodNode.filterLambda {
                val nodeName = it.name
                val nodeDesc = it.desc

                // 在配置的hook点列表中查找是否有与lambda匹配的
                val find = config.hookPointList.find { point ->
                    nodeName == point.methodName && nodeDesc.endsWith(point.interfaceSignSuffix)
                }
                find != null
            }

            // 对于找到的lambda，获取其实际执行的方法并加入到hook列表中
            dynamicInsnNodes.forEach {
                val handle = it.bsmArgs[1] as? Handle
                if (handle != null) {
                    //找到 lambda 指向的目标方法
                    val nameWithDesc = handle.name + handle.desc
                    val method = methods.find { it.nameWithDesc == nameWithDesc }!!
                    shouldHookMethodList.add(method)
                    println("ClickPlugin visitEnd lambda hook")
                }
            }
        }

        // 对所有收集到的需要hook的方法执行hook操作
        shouldHookMethodList.forEach {
            hookMethod(modeNode = it)
        }
        println("ClickPlugin shouldHookMethodList size = ${shouldHookMethodList.size}")

        // 将修改后的字节码传递给下一个ClassVisitor进行进一步的处理或写入
        accept(nextClassVisitor)
    }


    /**
     * 对指定方法进行hook，插入点击事件的检查。
     *
     * @param modeNode 需要被hook的方法节点。
     */
    private fun hookMethod(modeNode: MethodNode) {
        // 获取方法参数的类型
        val argumentTypes = Type.getArgumentTypes(modeNode.desc)

        // 查找第一个参数的类型为View的参数索引
        val viewArgumentIndex = argumentTypes?.indexOfFirst {
            it.descriptor == ViewDescriptor
        } ?: -1

        // 如果找到了类型为View的参数
        if (viewArgumentIndex >= 0) {
            println("ClickPlugin hookMethod viewArgumentIndex >= 0")

            val instructions = modeNode.instructions

            // 确保指令集不为空
            if (instructions != null && instructions.size() > 0) {
                println("ClickPlugin hookMethod 指令集不为空")

                val list = InsnList()

                // 加载View类型的参数
                list.add(
                    VarInsnNode(
                        Opcodes.ALOAD, getVisitPosition(
                            argumentTypes,
                            viewArgumentIndex,
                            modeNode.isStatic
                        )
                    )
                )

                // 调用静态的点击事件检查方法
                list.add(
                    MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        config.clickCheckClass,
                        config.onClickMethodName,
                        config.onClickMethodDesc
                    )
                )

                // 根据点击事件检查方法的结果进行跳转。如果结果为非0（即true），跳过后续的RETURN指令
                val labelNode = LabelNode()
                list.add(JumpInsnNode(Opcodes.IFNE, labelNode))

                // 如果点击事件检查方法的结果为0（即false），则直接返回，不执行原方法的后续指令
                list.add(InsnNode(Opcodes.RETURN))

                // 跳转标签的位置
                list.add(labelNode)

                // 将新的指令集插入到原方法的指令集的起始位置
                instructions.insert(list)
            }
        }
    }


    /**
     * 获取指定参数在局部变量表中的位置。
     *
     * @param argumentTypes 所有参数的类型数组。
     * @param parameterIndex 指定参数的索引位置。
     * @param isStaticMethod 方法是否是静态的。
     * @return 指定参数在局部变量表中的位置。
     */
    private fun getVisitPosition(
        argumentTypes: Array<Type>,
        parameterIndex: Int,
        isStaticMethod: Boolean
    ): Int {
        // 如果参数索引无效，则抛出错误。
        if (parameterIndex < 0 || parameterIndex >= argumentTypes.size) {
            throw Error("getVisitPosition error")
        }

        // 如果参数是列表中的第一个
        return if (parameterIndex == 0) {
            // 对于静态方法，第一个参数的位置是 0；对于非静态方法，由于存在 `this` 引用，第一个参数的位置是 1。
            if (isStaticMethod) {
                0
            } else {
                1
            }
        } else {
            // 对于其他参数，其位置是前一个参数的位置加上前一个参数的大小（某些类型，如 double 和 long，会占用两个位置）。
            getVisitPosition(
                argumentTypes,
                parameterIndex - 1,
                isStaticMethod
            ) + argumentTypes[parameterIndex - 1].size
        }
    }

    private fun MethodNode.isHookPoint(): Boolean {
        val myInterfaces = interfaces
        if (myInterfaces.isNullOrEmpty()) {
            return false
        }
        val extraHookMethodList = config.hookPointList
        extraHookMethodList.forEach {
            if (myInterfaces.contains(it.interfaceName) && this.nameWithDesc == it.nameWithDesc) {
                return true
            }
        }
        return false
    }

    private fun MethodNode.hasCheckViewAnnotation(): Boolean {
        return hasAnnotation(config.checkViewOnClickAnnotation)
    }

    private fun MethodNode.hasUncheckViewOnClickAnnotation(): Boolean {
        return hasAnnotation(config.uncheckViewOnClickAnnotation)
    }

    private fun MethodNode.hasButterKnifeOnClickAnnotation(): Boolean {
        return hasAnnotation(ButterKnifeOnClickAnnotationDesc)
    }
}