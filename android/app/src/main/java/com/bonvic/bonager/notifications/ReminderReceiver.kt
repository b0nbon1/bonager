package com.bonvic.bonager.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bonvic.bonager.data.BonagerRepository

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(ReminderManager.EXTRA_TASK_TITLE) ?: "Task"
        if (id >= 0) ReminderManager(context).showTaskReminder(id, title)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val result = goAsync()
        Thread {
            try {
                BonagerRepository(context).rescheduleReminders()
            } finally {
                result.finish()
            }
        }.start()
    }
}
