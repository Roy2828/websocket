package com.nofish.plugins

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

/**
 * 定义用于常见的ASM操作的常量
 */
internal const val InitMethodName = "<init>"

/**
 * 不包括包路径的部分
 */
internal val ClassNode.simpleClassName: String
    get() = name.substringAfterLast('/')

/**
 * 返回方法的名称和描述符
 */
internal val MethodNode.nameWithDesc: String
    get() = name + desc

/**
 * 是否表示一个静态方法
 */
internal val MethodNode.isStatic: Boolean
    get() = access and Opcodes.ACC_STATIC != 0

/**
 * 是否表示一个构造方法
 */
internal val MethodNode.isInitMethod: Boolean
    get() = name == InitMethodName

/**
 * 是否有给定描述符的注解
 */
internal fun MethodNode.hasAnnotation(annotationDesc: String): Boolean {
    return visibleAnnotations?.find { it.desc == annotationDesc } != null
}

/**
 * 修改MethodInsnNode的描述符以添加一个新的参数
 * @param argumentType 需要添加为新参数的类。
 */
internal fun MethodInsnNode.insertArgument(argumentType: Class<*>) {
    // 获取当前方法指令的类型（包括参数和返回值）。
    val type = Type.getMethodType(desc)
    // 获取当前方法的所有参数类型。
    val argumentTypes = type.argumentTypes
    // 获取当前方法的返回类型。
    val returnType = type.returnType
    // 创建一个新的参数数组，长度比原始数组多一个（用于新参数）。
    val newArgumentTypes = arrayOfNulls<Type>(argumentTypes.size + 1)
    // 将原始参数数组的内容复制到新的参数数组中。
    System.arraycopy(argumentTypes, 0, newArgumentTypes, 0, argumentTypes.size)
    // 将新的参数类型设置为新数组的最后一个元素。
    newArgumentTypes[newArgumentTypes.size - 1] = Type.getType(argumentType)
    // 使用新的参数数组和原始的返回类型来生成一个新的方法描述符，并设置到当前方法指令中。
    desc = Type.getMethodDescriptor(returnType, *newArgumentTypes)
}

/**
 * 从MethodNode中过滤Lambda表达式
 */
internal fun MethodNode.filterLambda(filter: (InvokeDynamicInsnNode) -> Boolean): List<InvokeDynamicInsnNode> {
    val mInstructions = instructions ?: return emptyList()
    val dynamicList = mutableListOf<InvokeDynamicInsnNode>()
    mInstructions.forEach { instruction ->
        if (instruction is InvokeDynamicInsnNode) {
            if (filter(instruction)) {
                dynamicList.add(instruction)
            }
        }
    }
    return dynamicList
}