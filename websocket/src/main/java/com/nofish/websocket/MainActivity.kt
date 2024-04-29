package com.nofish.websocket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nofish.websocket.subscribe.WsModel
import com.nofish.websocket.subscribe.WsSubscribe

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WsManager.openWs()
        WsManager.register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        WsManager.unregister(this)
    }

    @WsSubscribe(WsModel.CHAT)
    fun onChatMessage(any: Any) {

    }
}