package app.expressiveclock.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.expressiveclock.R
import app.expressiveclock.core.Alarm
import app.expressiveclock.data.ClockStore
import java.time.Instant
import java.time.ZoneId

internal object UpcomingAlarm {
    const val SHOW = "app.expressiveclock.UPCOMING"
    const val SKIP = "app.expressiveclock.SKIP_OCCURRENCE"
    const val AT = "occurrence"
    private const val CHANNEL = "upcoming_alarms"
    private fun tag(id: Long) = "upcoming:$id"
    private fun intent(context: Context, id: Long, at: Long, action: String) =
        Intent(context, AlarmReceiver::class.java).setAction(action)
            .setData(Uri.parse("expressiveclock://$action/$id${if (action == SKIP) "/$at" else ""}"))
            .putExtra(AlarmReceiver.EXTRA_ID, id).putExtra(AT, at)
    private fun pending(context: Context, id: Long, at: Long, action: String) =
        PendingIntent.getBroadcast(context, 0, intent(context,id,at,action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun hide(context: Context, id: Long) { NotificationManagerCompat.from(context).cancel(tag(id), 0) }
    fun cancel(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context,id,0,SHOW))
        hide(context,id)
    }
    fun schedule(context: Context, alarm: Alarm, at: Long) {
        cancel(context,alarm.id)
        val whenMillis = at - 30 * 60_000L
        if (whenMillis <= System.currentTimeMillis()) show(context,alarm,at)
        else {
            val manager = context.getSystemService(AlarmManager::class.java)
            val pi = pending(context,alarm.id,at,SHOW)
            if (AlarmScheduler.canScheduleExact(context)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,whenMillis,pi)
            else manager.setWindow(AlarmManager.RTC_WAKEUP,whenMillis,60_000L,pi)
        }
    }
    fun matches(alarm: Alarm, at: Long): Boolean {
        if (!alarm.enabled || alarm.skippedOccurrenceMillis == at) return false
        val before = Instant.ofEpochMilli(at - 1).atZone(ZoneId.systemDefault())
        return alarm.nextFire(before)?.toInstant()?.toEpochMilli() == at
    }
    private fun show(context: Context, alarm: Alarm, at: Long) {
        if (!matches(alarm,at) || at <= System.currentTimeMillis()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Upcoming alarms",NotificationManager.IMPORTANCE_LOW).apply { setSound(null,null) })
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val time = java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()))
        val n = NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Upcoming alarm · $time").setContentText(alarm.label.ifBlank { "Alarm" })
            .setContentIntent(Notifications.openApp(context, Notifications.TAB_ALARMS))
            .setSilent(true).setTimeoutAfter((at-System.currentTimeMillis()).coerceAtLeast(1))
            .addAction(0,"Dismiss this occurrence",pending(context,alarm.id,at,SKIP)).build()
        try { NotificationManagerCompat.from(context).notify(tag(alarm.id),0,n) } catch (_: SecurityException) { }
    }
    suspend fun receive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmReceiver.EXTRA_ID,-1)
        val at = intent.getLongExtra(AT,-1)
        val store = ClockStore(context)
        if (intent.action == SHOW) {
            store.alarmsNow().firstOrNull { it.id == id }?.let { show(context,it,at) }
        } else {
            val updated = store.updateAlarms { list -> list.map { alarm ->
                if (alarm.id != id || !matches(alarm,at) || at <= System.currentTimeMillis()) alarm
                else if (alarm.isOneShot) alarm.copy(enabled=false,snoozedUntilMillis=null,skippedOccurrenceMillis=at)
                else alarm.copy(skippedOccurrenceMillis=at,skippedOccurrenceDate=Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate(),snoozedUntilMillis=null)
            } }
            hide(context,id)
            updated.firstOrNull { it.id == id }?.let { AlarmScheduler.schedule(context,it) }
        }
    }
}
