package com.example.haptictester

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.view.HapticFeedbackConstants.*
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.haptictester.databinding.ActivityTestViewHapticBinding

data class HapticAction(val name: String, val value: Int)

class TestViewHapticActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestViewHapticBinding
    private val actionList = ArrayList<HapticAction>()
    private var isHapticEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestViewHapticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initHapticActions()
        val adapter = HapticFeedbackAdapter(this, R.layout.action_item, actionList)
        binding.actionListView.adapter = adapter
        binding.actionListView.setOnItemClickListener { _, view, position, _ ->
            val action = actionList[position]
            view.isHapticFeedbackEnabled = isHapticEnabled
            // flags: 0, FLAG_IGNORE_VIEW_SETTING, FLAG_IGNORE_GLOBAL_SETTING
            //  0 - respect the settings
            view.performHapticFeedback(action.value, 0)
            Utils.showToast("Feeling: ${action.name} - ID: ${action.value}")
        }

        binding.toggleHaptic.setOnCheckedChangeListener { _, isChecked ->
            isHapticEnabled = isChecked
            binding.isHapticFeedbackEnabled.text = "isHapticFeedbackEnabled: $isChecked"
        }
        binding.toggleHaptic.performClick() // Initially set ON
    }

    // https://developer.android.google.cn/reference/kotlin/android/view/HapticFeedbackConstants
    private fun initHapticActions() {
        actionList.apply {
            add(HapticAction("CLOCK_TICK", CLOCK_TICK))
            add(HapticAction("CONFIRM", CONFIRM))
            add(HapticAction("CONTEXT_CLICK", CONTEXT_CLICK))
            add(HapticAction("GESTURE_START", GESTURE_START))
            add(HapticAction("GESTURE_END", GESTURE_END))
            add(HapticAction("KEYBOARD_PRESS", KEYBOARD_PRESS))
            add(HapticAction("KEYBOARD_RELEASE", KEYBOARD_RELEASE))
            add(HapticAction("KEYBOARD_TAP", KEYBOARD_TAP))
            add(HapticAction("LONG_PRESS", LONG_PRESS))
            add(HapticAction("REJECT", REJECT))
            add(HapticAction("TEXT_HANDLE_MOVE", TEXT_HANDLE_MOVE))
            add(HapticAction("VIRTUAL_KEY", VIRTUAL_KEY))
            add(HapticAction("VIRTUAL_KEY_RELEASE", VIRTUAL_KEY_RELEASE))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sub_act, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.close -> finish()
        }
        return true
    }
}

class HapticFeedbackAdapter(activity: Activity, private val resourceId: Int, data: List<HapticAction>) :
        ArrayAdapter<HapticAction>(activity, resourceId, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(resourceId, parent, false)
        val actionName = view.findViewById<TextView>(R.id.actionName)
        actionName.text = getItem(position)?.name
        return view
    }
}