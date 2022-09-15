package com.example.haptictester

import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity
import com.example.haptictester.databinding.ActivityTestVibratorMgrBinding

/**
 * https://developer.android.google.cn/guide/topics/manifest/uses-sdk-element.html?hl=en#ApiLevels
 * https://developer.android.google.cn/reference/kotlin/android/os/VibratorManager
*/
class TestVibratorMgrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestVibratorMgrBinding
    private var vibrator: Vibrator? = null
    private var _vibratorMgr: VibratorManager? = null
    private val vibratorMgr: VibratorManager get() = _vibratorMgr!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestVibratorMgrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val info = StringBuilder()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12
            _vibratorMgr = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorMgr.defaultVibrator

            val vibIds = vibratorMgr.vibratorIds
            info.append("Vibrator count: ${vibIds.size}")
            for (e in vibratorMgr.vibratorIds) {
                val vib = vibratorMgr.getVibrator(e)
                info.append("\nID: $e -- hasVibrator: ${vib.hasVibrator()}, its ID: ${vib.id}")
            }
        }
        else {
            info.append("Vibrator Manager is not available on a version of Android earlier than Android 12.")
            vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        binding.vibInfoGeneral.text = info.toString()
    }
}