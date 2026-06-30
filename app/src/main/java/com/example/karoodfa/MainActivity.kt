package com.example.karoodfa

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = "DFA a1 Extension is installed!\n\nTo pair your heart rate strap, go to Settings -> Sensors -> Extensions on your Karoo."
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(48, 48, 48, 48)
        }
        setContentView(textView)
    }
}
