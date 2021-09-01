package com.xiaoma.processdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.xiaoma.processmodule.ProcessApi

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn1).setOnClickListener {
            ProcessApi.getInstance().clientInit(this@MainActivity)
        }
    }
}