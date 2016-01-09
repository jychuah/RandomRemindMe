package chuah.is_a_teacher.com.randomremindme;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class AlarmUpdateService extends Service {

    public static String ACTION_SET_UPDATE = "chuah.is_a_teacher.com.randomremindme.AlarmUpdateService.ACTION_SET_UPDATE";

    public AlarmUpdateService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_SET_UPDATE)) {
            long millisecondsLeft = MainActivity.nextAlarmTime - System.currentTimeMillis();

            int timeLeft = 5;

            String[] times = getResources().getStringArray(R.array.randomization);
            for (String time : times) {
                if (millisecondsLeft > (long)Integer.parseInt(time) * 60 * 1000) {
                    timeLeft = Integer.parseInt(time);
                }
            }
            AlarmSetNotifier.notifyListeners(MainActivity.nextAlarmTime);
            if (timeLeft >= 5) {
                Intent updateIntent = new Intent(this, AlarmUpdateService.class);
                updateIntent.setAction(ACTION_SET_UPDATE);
                ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, MainActivity.nextAlarmTime - 1000 * 60 - timeLeft * 1000 * 60, PendingIntent.getBroadcast(this, 0, updateIntent, 0));
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
