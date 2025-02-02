package com.example.haptictester

import android.hardware.input.InputManager
import android.media.audiofx.HapticGenerator
import android.os.*
import android.util.Log
import android.view.InputDevice
import android.view.InputDevice.SOURCE_GAMEPAD
import androidx.appcompat.app.AppCompatActivity
import com.example.haptictester.databinding.ActivityTestVibratorMgrBinding

/**
 * https://developer.android.google.cn/guide/topics/manifest/uses-sdk-element.html?hl=en#ApiLevels
 * https://developer.android.google.cn/reference/kotlin/android/os/VibratorManager
 * https://developer.android.google.cn/reference/kotlin/android/hardware/input/InputManager
*/
class TestVibratorMgrActivity : AppCompatActivity(), InputManager.InputDeviceListener {

    private val TAG = "InputDeviceTest"
    private lateinit var binding: ActivityTestVibratorMgrBinding

    private var vibratorDefault: Vibrator? = null
    private var vibrator: Vibrator? = null
    private var vibratorSecond: Vibrator? = null
    private var _vibratorMgr: VibratorManager? = null
    private val vibratorMgr: VibratorManager get() = _vibratorMgr!!

    private lateinit var inputMgr: InputManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestVibratorMgrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inputMgr = getSystemService(INPUT_SERVICE) as InputManager
        inputMgr.registerInputDeviceListener(this, null)

        val info = StringBuilder()
        // VibratorManager可以管理“当前输入设备”的多个马达
        // 通过VibratorManager无法获取外接的Game Controller!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12
            _vibratorMgr = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorDefault = vibratorMgr.defaultVibrator

            val vibIds = vibratorMgr.vibratorIds
            info.append("Vibrator count: ${vibIds.size}")
            for (e in vibIds) {
                val vib = vibratorMgr.getVibrator(e)
                info.append("\nID: $e -- hasVibrator: ${vib.hasVibrator()}, its ID: ${vib.id}")
            }
        }
        else {
            info.append("Vibrator Manager is not available on a version of Android earlier than Android 12.")
            vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        binding.vibInfoGeneral.text = info.toString()

        binding.btnVibrateDefault.setOnClickListener {
            // By default it vibrates on the phone
            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                TODO("VERSION.SDK_INT < O")
            }
            vibratorDefault?.vibrate(effect)
        }

        // 测试输入设备 InputDevice
        // fun getDescriptor(): String!
        // An input device descriptor uniquely identifies an input device. Its value is intended to be persistent across system restarts,
        // and should not change even if the input device is disconnected, reconnected or reconfigured at any time.
        binding.btnEnumerate.setOnClickListener {
            info.setLength(0)
            vibrator = null
            vibratorSecond = null
            // 方法一：通过InputDevice的静态方法来枚举
            /*val devIds = InputDevice.getDeviceIds()
            info.append("Device count: ${devIds.size}")
            for (e in devIds) {
                val dev = InputDevice.getDevice(e)
                info.append("\n${dev.toString()}")
                //info.append("\nID: $e, hasVibrator: ${dev.vibrator.hasVibrator()}")
                if (dev.vibrator.hasVibrator()) {
                    vibrator = dev.vibrator
                    info.append("\n@@@ A vibrator detected @@@")
                }
            }*/

            // 方法二：通过InputManager来枚举“所有的”输入设备
            val devIds = inputMgr.inputDeviceIds
            info.append("Device count: ${devIds.size}")
            for (e in devIds) {
                val dev = inputMgr.getInputDevice(e)
                info.append("\n${dev.toString()}")
                val srcType = dev.sources and SOURCE_GAMEPAD
                if (srcType == SOURCE_GAMEPAD) {
                    info.append("@@@The device above is a Gamepad. Controller num: ${dev.controllerNumber}\n")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12
                    val vibIds = dev.vibratorManager.vibratorIds
                    if (vibIds.isNotEmpty()) {
                        info.append("\nInput Device of ID $e has ${vibIds.size} vibrator(s)")
                        for (d in vibIds) {
                            val vib = dev.vibratorManager.getVibrator(d)
                            info.append("\nVibrator ID: $d -- hasVibrator: ${vib.hasVibrator()}, its ID: ${vib.id}")
                            if (vibrator != null) { // saved for later use. 最多保存两个马达
                                vibratorSecond = vib
                            } else {
                                vibrator = vib
                            }
                        }
                    }
                } else {
                    // 这里不能支持单个输入设备包含多马达的情况
                    if (dev.vibrator.hasVibrator()) {
                        vibrator = dev.vibrator // saved for later use
                        info.append("\n@@@ A vibrator detected @@@")
                    }
                }
            }
            binding.inputDeviceInfo.text = info.toString()
        }

        binding.btnVibrate.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0
                val effect1 = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator?.vibrate(effect1)
                vibratorSecond?.let {
                    Thread.sleep(2000)
                    val effect2 = VibrationEffect.createOneShot(1000, 255)
                    it.vibrate(effect2)
                }
            }
        }

        binding.btnHapticGen.setOnClickListener {
            // https://developer.android.google.cn/reference/kotlin/android/media/audiofx/HapticGenerator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12
                val avail = HapticGenerator.isAvailable()
                Log.i(TAG, "HapticGenerator.isAvailable $avail")

                val generator = HapticGenerator.create(0) // MediaPlayer.getAudioSessionId()
                generator.enabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inputMgr.unregisterInputDeviceListener(this)
    }

    // 监听输入设备的连接/断开
    override fun onInputDeviceAdded(deviceId: Int) {
        // Called whenever an input device has been added to the system.
        Log.d(TAG, "onInputDeviceAdded")
        val dev = inputMgr.getInputDevice(deviceId)
        if ((dev.sources and SOURCE_GAMEPAD) == SOURCE_GAMEPAD && dev.vibrator.hasVibrator()) {
            Log.d(TAG, "A Gamepad connected. Descriptor: ${dev.descriptor}")
        }
    }
    override fun onInputDeviceRemoved(deviceId: Int) {
        // Called whenever an input device has been removed from the system.
        Log.d(TAG, "onInputDeviceRemoved")
        val dev = inputMgr.getInputDevice(deviceId) // null
    }
    override fun onInputDeviceChanged(deviceId: Int) {
        // Called whenever the properties of an input device have changed since they were last queried.
        Log.d(TAG, "onInputDeviceChanged")
    }

}