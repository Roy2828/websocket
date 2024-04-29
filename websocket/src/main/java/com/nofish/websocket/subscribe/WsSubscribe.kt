package com.nofish.websocket.subscribe

import com.nofish.websocket.subscribe.WsModel


/**
 * TODO sticky 粘性事件
 * TODO Thread 线程切换
 * TODO priority 排序
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class WsSubscribe(val model: WsModel, val priority: Int = 0) {

}



