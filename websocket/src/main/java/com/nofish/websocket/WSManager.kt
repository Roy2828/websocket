package com.nofish.websocket

import android.net.ConnectivityManager
import android.net.Network
import android.os.Looper
import android.util.Log
import com.nofish.websocket.subscribe.WsModel
import com.nofish.websocket.subscribe.WsSubscribe
import com.nofish.websocket.subscribe.WsSubscriberMethod
import com.nofish.websocket.subscribe.WsSubscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.util.EnumMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.math.pow

private const val MODIFIERS_IGNORE = Modifier.ABSTRACT or Modifier.STATIC

object WsManager : CoroutineScope by MainScope() {
    val TAG = "WsManager"
    private val wsHttpClient by lazy {
        OkHttpClient.Builder()
            .pingInterval(10, TimeUnit.SECONDS) // 设置 PING 帧发送间隔
            .build()
    }
    private val requestHttp by lazy {
        Request.Builder()
            .url("ws://xixixixixix")
            .build()
    }
    var mWebSocket: WebSocket? = null
        private set

    private val subscriptionsByModelType: MutableMap<WsModel, CopyOnWriteArrayList<WsSubscription>> =
        EnumMap(WsModel::class.java)
    private val typesBySubscriber: MutableMap<Any, MutableList<WsModel>> = hashMapOf()

    private val currentPostingThreadState: ThreadLocal<PostingThreadState> =
        object : ThreadLocal<PostingThreadState>() {
            override fun initialValue(): PostingThreadState {
                return PostingThreadState()
            }
        }

    private var reconnectJob: Job? = null
    private var connectionStatus = ConnectionStatus.DISCONNECTED // 初始状态为断开连接
    private val mMaxRetryCount = 10 // 最大重试次数
    private val mNetWorkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // 联网后开始重连
                Log.e(TAG, "网络恢复")
                reconnect()
            }

            override fun onLost(network: Network) {
                // 断网停止重连
                if (!NetworkStatusMonitor.isNetworkConnected()) {
                    Log.e(TAG, "网络断开")
                    cancelReconnect()
                }
            }
        }
    }
    private fun cancelReconnect() {
        Log.e(TAG, "取消重连")
        connectionStatus = ConnectionStatus.DISCONNECTED
        reconnectJob?.cancel()
    }

    private fun reconnect() {
        if (reconnectJob?.isActive == true) {
            // 避免重复执行重连逻辑
            return
        }
        connectionStatus = ConnectionStatus.RECONNECTING
        reconnectJob = launch(Dispatchers.IO) {
            var retryCount = 0
            while (retryCount <= mMaxRetryCount) {
//                if (!NetworkStatusMonitor.isNetworkConnected()) {
//                    Log.e(TAG, "reconnect isNetworkNotConnected")
//                    break
//                }
                if (retryCount == mMaxRetryCount) {
                    Log.e(TAG, "超过最大重试次数，停止重连")
                    break
                }
                if (connectionStatus != ConnectionStatus.CONNECTED) {
                    // 进行重连
                    connect()
                    Log.e(TAG, "尝试重连")
                    retryCount++
                } else {
                    Log.e(TAG, "重连成功")
                    // 连接成功，退出重连循环
                    break
                }
                delay(exponentialBackoffRetry(retryCount))
            }
        }
    }

    private fun exponentialBackoffRetry(retryCount: Int): Long {
        val maxRetryDelay = 10000L // 最大重试延迟时间（毫秒）
        val baseDelay = 200L // 基础延迟时间（毫秒）
        val multiplier = 1.2 // 延迟时间乘数
        val delay = baseDelay * multiplier.pow(retryCount.toDouble()).toLong()
        Log.e(TAG, "重连间隔变为 $delay")
        return minOf(delay, maxRetryDelay)
    }

    fun openWs() {
        if (connectionStatus == ConnectionStatus.RECONNECTING || connectionStatus == ConnectionStatus.CONNECTED) {
            return
        }
        Log.e(TAG, "openWs")
        connect()
        NetworkStatusMonitor.register(mNetWorkCallback)
    }

    private fun connect() {
        Log.e(TAG, "开始连接 WS")
        mWebSocket = wsHttpClient.newWebSocket(requestHttp, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                Log.e(TAG, "WS connection successful")
                connectionStatus = ConnectionStatus.CONNECTED
                // WebSocket 连接建立
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                Log.e(TAG, "openWs onMessage $text")
                try {
//                    post()
                } catch (e: ClassCastException) {
                    Log.e(TAG, "openWs onMessage error $e")
                }
                // 收到服务端发送来的 String 类型消息
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                // 收到服务端发送来的 ByteString 类型消息
                Log.e(TAG, "openWs onMessage bytes $bytes")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                Log.e(TAG, "openWs onClosing")
                mWebSocket = null
                // 收到服务端发来的 CLOSE 帧消息，准备关闭连接
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                Log.e(TAG, "openWs onClosed")
                mWebSocket = null
                // WebSocket 连接关闭
                /**
                 *  Defined Status Codes

                Endpoints MAY use the following pre-defined status codes when sending
                a Close frame.

                1000

                1000 indicates a normal closure, meaning that the purpose for
                which the connection was established has been fulfilled.

                1001

                1001 indicates that an endpoint is "going away", such as a server
                going down or a browser having navigated away from a page.

                1002

                1002 indicates that an endpoint is terminating the connection due
                to a protocol error.

                1003

                1003 indicates that an endpoint is terminating the connection
                because it has received a type of data it cannot accept (e.g., an
                endpoint that understands only text data MAY send this if it
                receives a binary message).
                 */

                // Not normally close, need to reconnect
                if (code != 1000) {
                    reconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                Log.e(TAG, "Ws连接失败")
                mWebSocket = null
                // 出错了
                reconnect()
            }
        })
    }


    /**
     * 注册订阅者对象，将对象中带有 @WsSubscribe 注解的订阅方法添加到订阅列表中
     *
     * @param subscriber 要注册的订阅者对象
     */
    fun register(subscriber: Any) {
        // 查找订阅者对象中的订阅方法
        val subscriberMethods: List<WsSubscriberMethod> = findSubscriberMethods(subscriber)

        // 对订阅操作进行同步，避免多线程竞争问题
        synchronized(this) {
            // 遍历订阅方法列表，将每个方法添加到订阅列表中
            for (subscriberMethod in subscriberMethods) {
                subscribe(subscriber, subscriberMethod)
            }
        }
    }


    private fun subscribe(subscriber: Any, subscriberMethod: WsSubscriberMethod) {
        // 获取订阅的模型类型
        val modelType: WsModel = subscriberMethod.modelType

        // 创建新的订阅对象
        val newSubscription = WsSubscription(subscriber, subscriberMethod)

        // 获取模型类型对应的订阅列表
        var subscriptions: CopyOnWriteArrayList<WsSubscription>? =
            subscriptionsByModelType[modelType]

        // 如果订阅列表为空，则创建一个新的订阅列表并将其关联到模型类型
        if (subscriptions == null) {
            subscriptions = CopyOnWriteArrayList<WsSubscription>()
            subscriptionsByModelType[modelType] = subscriptions
        } else {
            // 如果订阅列表不为空，检查是否已存在相同的订阅对象，若存在则抛出异常
            if (subscriptions.contains(newSubscription)) {
                throw IllegalArgumentException("Subscriber ${subscriber.javaClass} already registered to event $modelType")
            }
        }

        // 在合适的位置插入新的订阅对象，根据优先级从高到低排序
        val size = subscriptions.size
        for (i in 0..size) {
            if (i == size || subscriberMethod.priority > subscriptions[i].subscriberMethod.priority) {
                subscriptions.add(i, newSubscription)
                break
            }
        }

        // 更新订阅者订阅的事件列表
        var subscribedEvents: MutableList<WsModel>? = typesBySubscriber[subscriber]
        if (subscribedEvents == null) {
            subscribedEvents = ArrayList()
            typesBySubscriber[subscriber] = subscribedEvents
        }
        subscribedEvents.add(modelType)
    }

    @Synchronized
    fun isRegistered(subscriber: Any): Boolean {
        return typesBySubscriber.containsKey(subscriber)
    }

    @Synchronized
    fun unregister(subscriber: Any) {
        val subscribedTypes: List<WsModel>? = typesBySubscriber[subscriber]
        if (subscribedTypes != null) {
            for (eventType in subscribedTypes) {
                unsubscribeByEventType(subscriber, eventType)
            }
            typesBySubscriber.remove(subscriber)
        } else {
            Log.e(
                TAG,
                "WsManager Subscriber to unregister was not registered before: ${subscriber.javaClass}"
            )
        }
    }

    private fun unsubscribeByEventType(subscriber: Any, eventType: WsModel) {
        val subscriptions: CopyOnWriteArrayList<WsSubscription>? =
            subscriptionsByModelType[eventType]
        if (subscriptions != null) {
            var size = subscriptions.size
            var i = 0
            while (i < size) {
                val subscription: WsSubscription = subscriptions[i]
                if (subscription.subscriber === subscriber) {
                    subscriptions.removeAt(i)
                    i--
                    size--
                }
                i++
            }
        }
    }

    /**
     * 将给定的事件发布到事件总线。
     */
    private fun post(model: WsModel, event: Any) {
        // 获取当前线程的发布状态
        val postingState: PostingThreadState = currentPostingThreadState.get() as PostingThreadState
        // 获取事件队列
        val eventQueue: MutableList<Any> = postingState.eventQueue
        // 将事件添加到队列中
        eventQueue.add(event)

        // 如果当前没有正在发布事件，则开始进行事件发布
        if (!postingState.isPosting) {
            // 判断是否在主线程中发布事件
            postingState.isMainThread = Looper.getMainLooper() == Looper.myLooper()
            postingState.isPosting = true

            // 检查发布状态是否被取消
            if (postingState.canceled) {
                throw IllegalArgumentException("Internal error. Abort state was not reset")
            }

            try {
                // 循环处理事件队列中的事件
                while (eventQueue.isNotEmpty()) {
                    postSingleEvent(model, eventQueue.removeAt(0), postingState)
                }
            } finally {
                // 发布完成后重置发布状态
                postingState.isPosting = false
                postingState.isMainThread = false
            }
        }
    }

    /**
     * 将单个事件发布给其订阅者，针对特定的事件类型。
     */
    @Throws(Error::class)
    private fun postSingleEvent(model: WsModel, event: Any, postingState: PostingThreadState) {
        // 尝试发布事件并返回是否找到订阅者
        val subscriptionFound: Boolean = postSingleEventForEventType(event, postingState, model)
        // 如果没有订阅者，则记录日志
        if (!subscriptionFound) {
            Log.e(TAG, "WsManager No subscribers registered for event $model")
        }
    }

    /**
     * 将单个事件发布给其订阅者，针对给定的事件类型。
     * 如果存在订阅者，返回true；否则返回false。
     */
    private fun postSingleEventForEventType(
        event: Any,
        postingState: PostingThreadState,
        eventModel: WsModel
    ): Boolean {
        var subscriptions: CopyOnWriteArrayList<WsSubscription>?
        synchronized(this) { subscriptions = subscriptionsByModelType[eventModel] }
        Log.e(TAG, "subscriptions $subscriptions")

        if (!subscriptions.isNullOrEmpty()) {
            // 遍历订阅者列表，依次发布事件
            for (subscription in subscriptions!!) {
                postingState.event = event
                postingState.subscription = subscription

                // 发布事件并检查是否被取消
                val aborted: Boolean = try {
                    postToSubscription(subscription, event)
                    postingState.canceled
                } finally {
                    postingState.event = null
                    postingState.subscription = null
                    postingState.canceled = false
                }

                // 如果事件发布被取消，则终止发布过程
                if (aborted) {
                    break
                }
            }

            return true
        }
        return false
    }

    /**
     * 将事件发布给订阅者的具体方法。
     */
    private fun postToSubscription(
        subscription: WsSubscription,
        event: Any
    ) {
        try {
            // 使用反射调用订阅方法来处理事件
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event)
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "WsManager $e")
            // 处理订阅者异常（暂未实现）
//        handleSubscriberException(subscription, event, e.cause)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException("Unexpected exception", e)
        }
    }

    /**
     * 用于保存当前线程的发布状态的内部类。
     */
    internal class PostingThreadState {
        val eventQueue: MutableList<Any> = arrayListOf()
        var isPosting = false
        var isMainThread = false
        var subscription: WsSubscription? = null
        var event: Any? = null
        var canceled = false
    }

    /**
     * 查找对象中带有 @WsSubscribe 注解的订阅方法并返回它们的信息列表
     *
     * @param obj 要查找的对象
     * @return 订阅方法的信息列表
     */
    private fun findSubscriberMethods(obj: Any): List<WsSubscriberMethod> {
        // 存储订阅方法的列表
        val subscribers = mutableListOf<WsSubscriberMethod>()
        // 获取对象的类信息
        val objClass = obj.javaClass
        // 获取对象类中声明的所有方法
        val declaredMethods = objClass.declaredMethods
        // 遍历每个方法
        for (method in declaredMethods) {
            // 获取方法的修饰符
            val modifiers = method.modifiers
            // 构造完整的方法名，格式为 "类名.方法名"
            val methodName = "${method.declaringClass.name}.${method.name}"
            // 检查方法的修饰符，满足条件才进行处理
            if ((modifiers and Modifier.PUBLIC) != 0 && (modifiers and MODIFIERS_IGNORE) == 0) {
                // 获取方法的参数类型列表
                val parameterTypes = method.parameterTypes
                // 检查方法的参数个数，必须为 1
                if (parameterTypes.size == 1) {
                    // 获取方法上的 @WsSubscribe 注解
                    val subscribeAnnotation: WsSubscribe? =
                        method.getAnnotation(WsSubscribe::class.java)
                    // 检查注解是否存在
                    if (subscribeAnnotation != null) {
                        // 将订阅方法的信息封装为 WsSubscriberMethod 对象，并添加到列表中
                        subscribers.add(
                            WsSubscriberMethod(
                                subscribeAnnotation.model,
                                method,
                                subscribeAnnotation.priority,
                            )
                        )
                    }
                } else if (method.isAnnotationPresent(WsSubscribe::class.java)) {
                    // 参数个数不为 1，但方法上存在 @WsSubscribe 注解，抛出异常
                    throw IllegalArgumentException("@WsSubscribe method $methodName must have exactly 1 parameter but has ${parameterTypes.size}")
                }
            } else {
                // 方法的修饰符不符合要求，但方法上存在 @WsSubscribe 注解，抛出异常
                if (method.isAnnotationPresent(WsSubscribe::class.java)) {
                    throw IllegalArgumentException("$methodName is a illegal @WsSubscribe method: must be public, non-static, and non-abstract")
                }
            }
        }
        // 返回订阅方法的信息列表
        return subscribers
    }

}

enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    RECONNECTING
}
