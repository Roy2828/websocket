package com.nofish.asm_test

/**
 * 希望过滤的点击事件
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UncheckOnClick