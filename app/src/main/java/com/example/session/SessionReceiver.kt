package com.example.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** The alarm at the end of a session, and the Silence button in the notification. */
class SessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val session = FocusSession.get(context)
        when (intent.action) {
            ACTION_TIME_UP -> session.onTimeUp()
            ACTION_SILENCE -> session.silenceSession()
        }
    }

    companion object {
        const val ACTION_TIME_UP = "com.example.session.TIME_UP"
        const val ACTION_SILENCE = "com.example.session.SILENCE"
    }
}
