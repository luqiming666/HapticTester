package com.example.haptictester

import android.app.Application
import android.content.Context
import android.widget.Toast
import java.util.*

object Utils {
    fun isNumeric(str: String): Boolean {
        if (str.isEmpty()) return false
        var i = str.length
        while (--i >= 0) {
            if (!Character.isDigit(str[i])) {
                return false
            }
        }
        return true
    }

    fun getRandomNum(startNum: Int, endNum: Int): Int {
        if (endNum > startNum) {
            val random = Random()
            return random.nextInt(endNum - startNum) + startNum
        }
        return 0
    }

    fun showToast(text: String, duration: Int = Toast.LENGTH_SHORT): Unit {
        Toast.makeText(MyApp.context, text, duration).show()
    }
}

// A smart way to access App.context from anywhere
//  NOTE: remember to add android:name=".MyApp" to <application> in AndroidManifest.xml
class MyApp : Application() {
    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}