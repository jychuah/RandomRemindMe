package chuah.is_a_teacher.com.randomremindme;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Random;

public class RandomAlarmService extends Service {

    AlarmManager alarmManager;
    PendingIntent alarmIntent;
    Intent ringIntent;

    static final String nextAlarm = "chuah.is_a_teacher.com.randomremindme.RandomAlarmService.nextAlarm";
    static final String snooze = "chuah.is_a_teacher.com.randomremindme.RandomAlarmService.snooze";
    static final String cancelNext = "chuah.is_a_teacher.com.randomremindme.RandomAlarmService.cancelNext";
    public RandomAlarmService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (alarmIntent == null) {
            ringIntent = new Intent();
            ringIntent.setClass(this, MainActivity.AlarmReceiver.class);
            ringIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ringIntent.setAction(MainActivity.ring);
            alarmIntent = PendingIntent.getBroadcast(this, 0, ringIntent, 0);
        }

        if (intent.getAction() != null) {
            if (intent.getAction().equals(nextAlarm)) {
                if (enabled()) {
                    scheduleNextAlarm();
                }
            }
            if (intent.getAction().equals(snooze)) {
                if (enabled()) {
                    scheduleSnoozeAlarm();
                }
            }

            if (intent.getAction().equals(cancelNext)) {
                cancelNext();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public boolean enabled() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_enabled", false);
    }

    public void scheduleSnoozeAlarm() {
        Log.d("RandomAlarmService" , "Snooze intent received");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Random rand = new Random();
        int snooze_time = Integer.parseInt(prefs.getString("pref_snooze_time", "5"));
        boolean random_snooze = prefs.getBoolean("pref_random_snooze", false);

        if (random_snooze) {
            snooze_time = rand.nextInt(snooze_time) + 1;
        }

        scheduleAlarmAt(System.currentTimeMillis() + snooze_time * 1000 * 60);
    }

    public void scheduleNextAlarm() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Random rand = new Random();

        int frequency = Integer.parseInt(prefs.getString("pref_frequency", "120"));
        int randomization = Integer.parseInt(prefs.getString("pref_randomization", "30"));


        long start_time = prefs.getLong("pref_start_time", 3600000);
        long end_time = prefs.getLong("pref_end_time", 7200000);

        int random_interval = frequency + (-randomization + rand.nextInt(randomization * 2));
 //       int random_interval = 1;

        Calendar end = MainActivity.getLocalCalendar(end_time);
        Calendar start = MainActivity.getLocalCalendar(start_time);

        long alarmTime;

        long extent = System.currentTimeMillis() + (frequency + randomization) * 60 * 1000;


        if (start.getTimeInMillis() > System.currentTimeMillis()) {
            start.add(Calendar.MINUTE, random_interval);
            alarmTime = start.getTimeInMillis();
        } else {
            if (end.getTimeInMillis() < extent) {

                start.add(Calendar.DATE, 1);
                start.add(Calendar.MINUTE, random_interval);
                alarmTime = start.getTimeInMillis();
            } else {
                alarmTime = System.currentTimeMillis() + random_interval * 60 * 1000;
            }
        }


        scheduleAlarmAt(alarmTime);
    }

    public void cancelNext() {

        Log.d("RandomAlarmService", "Canceled next alarm");
        AlarmSetNotifier.notifyListeners(-1);
        alarmManager.cancel(alarmIntent);

    }

    public void scheduleAlarmAt(long alarmTime) {
        cancelNext();
        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);

        final long alarmTimePass = alarmTime;

        MainActivity.nextAlarmTime = alarmTime;

        Intent updateIntent = new Intent(this, AlarmUpdateService.class);
        updateIntent.setAction(AlarmUpdateService.ACTION_SET_UPDATE);
        startService(updateIntent);

        AlarmSetNotifier.notifyListeners(alarmTime);

        Handler h = new Handler();
        h.post(new Runnable() {
            @Override
            public void run() {
                Log.d("RandomAlarmService", "Next alarm at " + android.text.format.DateFormat.getTimeFormat(getApplicationContext()).format(new Date(alarmTimePass)));
                Toast.makeText(getApplicationContext(), "Next random alarm at " + android.text.format.DateFormat.getTimeFormat(getApplicationContext()).format(new Date(alarmTimePass)), Toast.LENGTH_LONG).show();
            }
        });
    }

}
