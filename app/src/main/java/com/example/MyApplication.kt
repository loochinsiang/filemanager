package com.example

import com.example.ui.components.LocaleAwareApplication

class MyApplication : LocaleAwareApplication() {
    override fun onCreate() {
        super.onCreate()
        // Here we can initialize any other global services safely
    }
}
