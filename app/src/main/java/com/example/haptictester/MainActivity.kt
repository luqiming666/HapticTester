package com.example.haptictester

import android.content.Intent
import android.os.*
import android.os.SystemClock.sleep
import android.os.VibrationEffect.*
import android.os.VibrationEffect.Composition.*
import android.os.Vibrator.VIBRATION_EFFECT_SUPPORT_YES
import android.util.Log
import android.view.HapticFeedbackConstants.CONTEXT_CLICK
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.haptictester.databinding.ActivityMainBinding
import kotlin.concurrent.thread


data class VibrationFeature(val name: String, val value: Int, var supported: Boolean = false)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val primitiveList = ArrayList<VibrationFeature>()
    private val effectList = ArrayList<VibrationFeature>()
    private lateinit var vibrator: Vibrator
    private var selectedEffect = 0
    private var repeatWaveform = -1 // -1 to disable repeating
    private var currentTiming = 20
    private var currentAmplitude = 50

    private var isMassaging = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start a new activity to test built-in haptic feedbacks on UI controls
        binding.btnTestNativeView.setOnClickListener {
            val intent = Intent(this, TestViewHapticActivity::class.java)
            startActivity(intent)
        }

        // Test Vibrator APIs
        // https://developer.android.google.cn/reference/android/os/Vibrator
        initVibrationPrimitives()
        initVibrationEffects()

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val info = StringBuilder()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0
            info.append("The vibrator has amplitude control: ${vibrator.hasAmplitudeControl()}")
            binding.vibInfoGeneral.text = info.toString()
            info.setLength(0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11
            info.append("Supported primitives (Android 11):")
            primitiveList.forEach {
                val supported = vibrator.arePrimitivesSupported(it.value)
                it.supported = supported[0]
                info.append("\n${it.name}: ${it.supported}")
            }
            binding.vibInfoPrimitives.text = info.toString()
            info.setLength(0)

            info.append("Supported predefined effects (Android 10):")
            effectList.forEach {
                val supported = vibrator.areEffectsSupported(it.value)
                it.supported = supported[0] == VIBRATION_EFFECT_SUPPORT_YES
                info.append("\n${it.name}: ${it.supported}")
            }
            binding.vibInfoPredefinedEffects.text = info.toString()
        }
        binding.btnTestPrimitives.setOnClickListener {
            Utils.showToast("TODO: need an Android 11 Google device to test!")
        }

        val newArray = effectList.map { it.name }
        binding.spinnerEffects.adapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, newArray)
        // NOTE: spinner设置监听器不能向listview或其他的组件一样直接使用lambda简化！
        binding.spinnerEffects.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                view?.performHapticFeedback(CONTEXT_CLICK) // test the built-in haptic feedback
                selectedEffect = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("Main-Spinner","Nothing is selected...")
            }
        }

        // Test createPredefined() and then vibrate(...)
        binding.btnPlayEffect.setOnClickListener {
            // The app should be in foreground for the vibration to happen. Background apps
            // should specify a ringtone, notification or alarm usage in order to vibrate.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10
                effectList[selectedEffect].let {
                    if (it.supported) {
                        val effect = VibrationEffect.createPredefined(it.value)
                        vibrator.vibrate(effect)
                        Utils.showToast("Can you feel ${it.name}?")
                    } else {
                        Utils.showToast("${it.name} not supported")
                    }
                }
            }
        }

        // Test the API createOneShot(), valid amplitude [1, 255]
        binding.btnTestOneShot.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0
                var duration = 500L
                var amplitude = DEFAULT_AMPLITUDE
                var reminder = false
                binding.shotDuration.text.toString().let {
                    if (Utils.isNumeric(it)) {
                        duration = Integer.parseInt(it).toLong()
                    } else {
                        reminder = true
                    }
                }
                binding.shotAmplitude.text.toString().let {
                    if (Utils.isNumeric(it)) {
                        amplitude = Integer.parseInt(it)
                    } else {
                        reminder = true
                    }
                }
                if (reminder) {
                    Utils.showToast("Please enter valid duration & amplitude above for testing. This time we use 500ms by default.")
                }
                val effect = VibrationEffect.createOneShot(duration, amplitude)
                vibrator.vibrate(effect)
            }
        }

        // Test the API createWaveform() - amplitude array auto-generated
        // The amplitude array of the generated waveform will be the same size as the given timing
        // array with alternating values of 0 (i.e. off) and DEFAULT_AMPLITUDE, starting with 0.
        //  So, the real effect: [silence, vibrate, silence, vibrate, ...]
        binding.btnTestWaveform.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0
                val timings = longArrayOf(0, 512, 128)
                val effect = VibrationEffect.createWaveform(timings, repeatWaveform)
                //val attr = VibrationAttributes.Builder().setUsage(USAGE_ALARM).build() // waiting for Android 13
                vibrator.vibrate(effect)
            }
        }
        binding.toggleRepeat.setOnCheckedChangeListener { _, isChecked ->
            repeatWaveform = if (isChecked) {
                0
            } else {
                vibrator.cancel()
                -1
            }
        }

        // Test the API createWaveform() - let's fill the amplitude array
        // Amplitude values must be between 0 and 255, and an amplitude of 0 implies no vibration (i.e. off).
        // Any pairs with a timing value of 0 will be ignored.
        binding.btnTestWaveform2.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0
                val vibTime = currentTiming * 5000L / 100
                val timings = longArrayOf(vibTime, (vibTime*0.2).toLong())
                val amplitudes = intArrayOf(currentAmplitude*255/100, 0) // [vibrate, silence]
                val effect = VibrationEffect.createWaveform(timings, amplitudes, repeatWaveform)
                vibrator.vibrate(effect)
            }
        }
        binding.barTiming.progress = currentTiming
        binding.barTiming.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentTiming = progress
                    if (progress == 0) currentTiming = 1 // otherwise createWaveform will crash
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
        binding.barAmplitude.progress = currentAmplitude
        binding.barAmplitude.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentAmplitude = progress
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        /////////////////////////////////////////////////////////////
        // Play for fun
        binding.btnMassage.setOnClickListener {
            if (isMassaging) {
                isMassaging = false
                vibrator.cancel()
                Utils.showToast("Massage is done ^_^")
            } else {
                isMassaging = true
                startMassage()
            }
        }
    }

    private fun initVibrationPrimitives() {
        primitiveList.apply {
            add(VibrationFeature("PRIMITIVE_CLICK", PRIMITIVE_CLICK))
            add(VibrationFeature("PRIMITIVE_THUD", PRIMITIVE_THUD))
            add(VibrationFeature("PRIMITIVE_SPIN", PRIMITIVE_SPIN))
            add(VibrationFeature("PRIMITIVE_QUICK_RISE", PRIMITIVE_QUICK_RISE))
            add(VibrationFeature("PRIMITIVE_SLOW_RISE", PRIMITIVE_SLOW_RISE))
            add(VibrationFeature("PRIMITIVE_QUICK_FALL", PRIMITIVE_QUICK_FALL))
            add(VibrationFeature("PRIMITIVE_TICK", PRIMITIVE_TICK))
            add(VibrationFeature("PRIMITIVE_LOW_TICK", PRIMITIVE_LOW_TICK))
        }
    }

    private fun initVibrationEffects() {
        effectList.apply {
            add(VibrationFeature("EFFECT_TICK", EFFECT_TICK))
            add(VibrationFeature("EFFECT_CLICK", EFFECT_CLICK))
            add(VibrationFeature("EFFECT_HEAVY_CLICK", EFFECT_HEAVY_CLICK))
            add(VibrationFeature("EFFECT_DOUBLE_CLICK", EFFECT_DOUBLE_CLICK))
        }
    }

    private val updateProgress = 1
    private val msgHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                updateProgress -> {
                    Utils.showToast(msg.obj as String)
                }
            }
        }
    }
    private fun startMassage() {
        thread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0
                val timings = longArrayOf(1000, 500)
                val amplitudes = intArrayOf(1, 0) // [vibrate, silence]
                while (isMassaging) {
                    val msg = Message().apply {
                        what = updateProgress
                        obj = "Mode switched - Timing [${timings[0]}, ${timings[1]}]"
                    }
                    msgHandler.sendMessage(msg)

                    val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                    vibrator.vibrate(effect) // It may go wrong when running in background?
                    sleep(5000)

                    // Generate a random number between [50, 1000]
                    timings[0] = Utils.getRandomNum(50, 1000).toLong()
                    when {
                        timings[0] > 700 -> {
                            timings[1] = 888
                        }
                        timings[0] > 400 -> {
                            timings[1] = 388
                        }
                        else -> {
                            timings[1] = 88
                        }
                    }
                }
            }
        }
    }

    private fun stopVibration() {
        isMassaging = false
        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopVibration()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about -> {
                AlertDialog.Builder(this).apply {
                    setTitle("About...")
                    setMessage("Version: ${BuildConfig.VERSION_NAME}")
                    setCancelable(true)
                    setPositiveButton("OK") { _, _ ->}
                    show()
                }
            }

            R.id.close -> finish()
        }
        return true
    }
}