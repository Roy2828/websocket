package com.nofish.websocket.subscribe

data class WsSubscription(var subscriber: Any, var subscriberMethod: WsSubscriberMethod) {

    override fun equals(other: Any?): Boolean {
        return if (other is WsSubscription) {
            val otherSubscription: WsSubscription = other
            (subscriber === otherSubscription.subscriber
                    && subscriberMethod == otherSubscription.subscriberMethod)
        } else {
            false
        }
    }
    override fun hashCode(): Int {
        return subscriber.hashCode() + subscriberMethod.methodString.hashCode()
    }


}