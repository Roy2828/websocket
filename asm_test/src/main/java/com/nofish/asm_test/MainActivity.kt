package com.nofish.asm_test

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val vClick01: Button = findViewById(R.id.btn_click_01)
        val vClick02: Button = findViewById(R.id.btn_click_02)
        val vClick03: Button = findViewById(R.id.btn_click_03)
        vClick01.setOnClickListener {
            Log.d("FUCK","btn_click_01")
        }

        vClick02.setOnClickListener(object : OnClickListener{
            @UncheckOnClick
            override fun onClick(v: View?) {
                Log.d("FUCK","btn_click_02")
            }
        })
        vClick03.setOnClickListener(object : OnClickListener{
            override fun onClick(v: View?) {
                Log.d("FUCK","btn_click_03")
            }
        })



    }

}