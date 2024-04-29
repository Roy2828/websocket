package com.nofish.websocket.subscribe

import java.lang.reflect.Method

data class WsSubscriberMethod(
    val modelType: WsModel,
    val method: Method,
    val priority: Int
) {
    var methodString: String? = null

    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else if (other is WsSubscriberMethod) {
            checkMethodString()
            val otherSubscriberMethod: WsSubscriberMethod = other
            otherSubscriberMethod.checkMethodString()
            // Don't use method.equals because of http://code.google.com/p/android/issues/detail?id=7811#c6
            methodString == otherSubscriberMethod.methodString
        } else {
            false
        }
    }

    @Synchronized
    private fun checkMethodString() {
        if (methodString == null) {
            // Method.toString has more overhead, just take relevant parts of the method
            methodString = StringBuilder(64)
                .append(method.declaringClass.name)
                .append('#').append(method.name)
                .append('(').append(modelType.name).toString()
        }
    }

    override fun hashCode(): Int {
        return method.hashCode()
    }
}