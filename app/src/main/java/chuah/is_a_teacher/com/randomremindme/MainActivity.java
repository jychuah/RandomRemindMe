package chuah.is_a_teacher.com.randomremindme;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements Ringer, AlarmSetNotifier.AlarmSetListener {


    static long nextAlarmTime = -1;

    Timer alarmAutoResetTimer = new Timer();

    SliderFragment slider = null;
    TimerFragment timer = null;
    static boolean isVisible = false;
    static MainActivity context = null;

    static boolean ringing = false;

    static String tag = "chuah.is_a_teacher.com.randomremindme";

    static String ring = "chuah.is_a_teacher.com.randomremiindme.MainActivity.ring";
    static String alarmRefresh = "chuah.is_a_teacher.com.randomremiindme.MainActivity.alarmRefresh";

    static Ringtone ringtone;
    static RingtoneManager ringtoneManager;


    public void onTimerClick(View v) {
        startActivity(new Intent(this, Settings.class));
    }


    public static Calendar getLocalCalendar(long time) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.HOUR_OF_DAY, (int)(time / 1000 / 60 / 60));
        c.set(Calendar.MINUTE, (int)(time / 1000 / 60 % 60));
        return c;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(tag, "Launching MainActivity");


        String ringTonePref = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_ringtone", "content://settings/system/notification_sound");

        Uri ringtoneUri = Uri.parse(ringTonePref);

        if (ringtoneManager == null) {
            ringtoneManager = new RingtoneManager(this);
        }

        isVisible = true;
        setContentView(R.layout.activity_main);
        context = this;

        Bundle args = new Bundle();

        if (getIntent().getAction().equals(ring)) {
            ring();
            args.putBoolean("ringing", true);

        } else {
            args.putBoolean("ringing", false);
        }
        if (savedInstanceState == null) {
            timer = new TimerFragment();
            timer.setArguments(args);
            slider = new SliderFragment();
            slider.setArguments(args);

            getFragmentManager().beginTransaction().add(R.id.timer, timer).commit();
            getFragmentManager().beginTransaction().add(R.id.slider, slider).commit();
        }
        RingEventReceiver.ringerList.add(this);
        AlarmSetNotifier.add(this);

        if (nextAlarmTime == -1 && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_enabled", false)) {
            Log.d(tag, "Schedule First Alarm");
            Intent scheduleFirstAlarmIntent = new Intent(this, RandomAlarmService.class);
            scheduleFirstAlarmIntent.setAction(RandomAlarmService.nextAlarm);
            startService(scheduleFirstAlarmIntent);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RingEventReceiver.ring);
        intentFilter.addAction(RingEventReceiver.cancelRing);
        registerReceiver(new RingEventReceiver(), intentFilter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void ring() {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String ringTonePref = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_ringtone", "content://settings/system/notification_sound");
        Uri ringtoneUri = Uri.parse(ringTonePref);
        if (ringtone == null) {
            ringtone = ringtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
            Log.d("ringtone", ringtone.toString());
        }

        Toast.makeText(this, PreferenceManager.getDefaultSharedPreferences(this).getString("pref_message", "Random Remind Me!"), Toast.LENGTH_LONG).show();
        Log.d(tag, "MainActivity ringing");

        ringing = true;

        ringtone.play();
        Vibrator vibrator =  (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator() && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_vibrate", false)) {
            vibrator.vibrate(1000);
        }

        TimerTask task = new TimerTask() {
            public void run() {
                MainActivity.context.runOnUiThread(new Runnable() {
                    public void run() {

                        Log.d(tag, "Alarm timeout");
                        sendCancelIntent();
                        MainActivity.context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        Intent refresh = new Intent(MainActivity.this, RandomAlarmService.class);
                        refresh.setAction(RandomAlarmService.nextAlarm);
                        startService(refresh);

                    }
                });
            }
        };

        long timerDuration = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_ring_duration", "30")) * 1000;


        alarmAutoResetTimer = new Timer();
        alarmAutoResetTimer.schedule(task, timerDuration);
    }

    public void killAutoReset() {
        alarmAutoResetTimer.cancel();
    }

    public void cancelRing() {

        Log.d(tag, "MainActivity cancelRinging");
        ringing = false;
        if (ringtone != null) {
            ringtoneManager.stopPreviousRingtone();
            ringtone.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    public void sendCancelIntent() {

        Intent cancelIntent = new Intent(this, RingEventReceiver.class);
        cancelIntent.setAction(RingEventReceiver.cancelRing);
        this.sendBroadcast(cancelIntent);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        RingEventReceiver.ringerList.remove(this);
        AlarmSetNotifier.remove(this);
    }

    public void alarmSet(long alarmTime) {
        this.nextAlarmTime = alarmTime;
    }

    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            PowerManager pm = (PowerManager)MainActivity.context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "");
            wl.acquire();


            Log.d(tag, "AlarmReceiver fired");

            if (!MainActivity.isVisible) {
                Intent ringIntent = new Intent(context, MainActivity.class);
                ringIntent.setAction(MainActivity.ring);
                MainActivity.context.startActivity(ringIntent);
            }

            Intent ringIntent = new Intent(context, RingEventReceiver.class);
            ringIntent.setAction(RingEventReceiver.ring);
            context.sendBroadcast(ringIntent);

            wl.release();

        }
    }

    public static class BootReceiver extends BroadcastReceiver {

        public BootReceiver() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(tag, "Boot Receiver");
            Log.d(tag, context.toString());
            Log.d(tag, intent.getAction());
            Toast.makeText(context, context.toString(), Toast.LENGTH_LONG).show();
            if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_enabled", false)) {

                    Intent alarmRefreshIntent = new Intent(context, RandomAlarmService.class);
                    alarmRefreshIntent.setAction(RandomAlarmService.nextAlarm);
                    try {
                        context.startService(alarmRefreshIntent);
                    } catch (Exception e) {
                        Log.d(tag, e.toString());
                    }

                }
            }
        }
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class TimerFragment extends Fragment implements Ringer, AlarmSetNotifier.AlarmSetListener {
        TextView timer_message = null;
        TextView status_message = null;
        SharedPreferences prefs;

        public TimerFragment() {

        }


        public void updateMessage() {
            if (timer_message != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                boolean enabled = preferences.getBoolean("pref_enabled", false);
                int frequency = Integer.parseInt(preferences.getString("pref_frequency", "120"));
                int randomization = Integer.parseInt(preferences.getString("pref_randomization", "30"));
                String ringtone = preferences.getString("pref_ringtone", "content://settings/system/notification_sound");
                long start_time = preferences.getLong("pref_start_time", 3600000);
                long end_time = preferences.getLong("pref_end_time", 7200000);
                String message = preferences.getString("pref_message", "Random Remind Me!");

                String msg = getString(R.string.frequency).replace("...", " " + frequency + " minutes ");
                msg += getString(R.string.random).toLowerCase().replace("...", " " + randomization + " minutes ");
                msg += getString(R.string.starttime).toLowerCase().replace("...", " " + android.text.format.DateFormat.getTimeFormat(getActivity()).format(new Date(getLocalCalendar(start_time).getTimeInMillis())) + " ");
                msg += getString(R.string.endingat).toLowerCase().replace("...", " " + android.text.format.DateFormat.getTimeFormat(getActivity()).format(new Date(getLocalCalendar(end_time).getTimeInMillis())) + " ");

                timer_message.setText(msg);
            }
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_timer, container, false);
            timer_message = (TextView) rootView.findViewById(R.id.timer_message);
            status_message = (TextView) rootView.findViewById(R.id.status_message);
            updateMessage();
            setNextStatusMessage();

            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences prefs, String s) {
                    updateMessage();
                    setNextStatusMessage();
                    if (!s.equals("pref_message") && !s.equals("pref_ringtone") &&
                            !s.equals("prefs_ring_duration") &&
                            !s.equals("prefs_snooze_time") &&
                            !s.equals("prefs_random_snooze") &&
                            prefs.getBoolean("pref_enabled", false)) {
                        Intent alarmRefreshIntent = new Intent(getActivity(), RandomAlarmService.class);
                        alarmRefreshIntent.setAction(RandomAlarmService.nextAlarm);
                        getActivity().startService(alarmRefreshIntent);
                    }
                }
            });

            RingEventReceiver.ringerList.add(this);
            AlarmSetNotifier.add(this);

            if (getArguments().getBoolean("ringing")) {
                ring();
            }

            return rootView;
        }
        public void ring() {
            Log.d(tag ,"TimerFragment ringing");
            status_message.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_message", "Random Remind Me!"));
        }

        public void setNextStatusMessage() {
            if (MainActivity.nextAlarmTime == -1 || !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_enabled", false)) {
                status_message.setText(getString(R.string.noalarm));
            } else {
                int minutesRemaining = (int)((nextAlarmTime - System.currentTimeMillis()) / 1000 / 60 % 60);
                long  millisecondsRemaining = (nextAlarmTime - System.currentTimeMillis());
                if (millisecondsRemaining > 60 * 1000 * 60) {
                    status_message.setText(R.string.morethantwo);
                } else {
                    String timeLeft = "";
                    String[] times = getResources().getStringArray(R.array.randomization);
                    for (int i = times.length - 1; i >= 0; i--) {
                        if (minutesRemaining <= Integer.parseInt(times[i])) {
                            timeLeft = times[i];
                        }
                    }
                    String msg = getString(R.string.lessthan) + " " + timeLeft + " " + getString(R.string.minutes);
                    status_message.setText(msg);
                }
            }
        }

        public void cancelRing() {
            setNextStatusMessage();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            RingEventReceiver.ringerList.remove(this);
        }

        @Override
        public void alarmSet(long alarmTime) {
            setNextStatusMessage();
        }
    }


    public static class SliderFragment extends Fragment implements View.OnDragListener, Ringer {
        ImageView alarmIconView = null;
        ImageView discardView = null;
        ImageView snoozeView = null;
        ImageView resetView = null;
        TextView helpText = null;
        View rootView = null;
        Animation alarmAnimation = null;

        boolean ringing = false;

        public SliderFragment() {

        }


        public void dragEnd() {
            alarmIconView.setVisibility(View.VISIBLE);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_slider, container, false);
            alarmIconView = (ImageView) rootView.findViewById(R.id.alarm_icon);
            discardView = (ImageView) rootView.findViewById(R.id.discard);
            snoozeView = (ImageView) rootView.findViewById(R.id.snooze);
            resetView = (ImageView) rootView.findViewById(R.id.reset);
            helpText = (TextView) rootView.findViewById(R.id.helptext);

            discardView.setOnDragListener(this);
            snoozeView.setOnDragListener(this);
            resetView.setOnDragListener(this);
            rootView.setOnDragListener(this);

            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int dimensions = Math.min(rootView.getHeight(), rootView.getWidth());
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dimensions, dimensions);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    rootView.setLayoutParams(params);
                }
            });


            alarmIconView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                        view.startDrag(null, shadowBuilder, view, 0);
                        view.setVisibility(View.INVISIBLE);
                        view.clearAnimation();
                        return true;

                    } else {
                        return false;
                    }
                }
            });

            alarmAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.swell);

            alarmAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    animation.start();

                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            RingEventReceiver.ringerList.add(this);

            if (getArguments().getBoolean("ringing")) {
                ring();
            }

            return rootView;
        }


        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            if (dragEvent.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                if (view == resetView) {
                    resetView.setImageResource(R.drawable.ic_lap_activated);
                    helpText.setText(R.string.reset);
                }
                if (view == discardView) {
                    discardView.setImageResource(R.drawable.ic_lockscreen_discard_activated);
                    helpText.setText(R.string.discard);
                }
                if (view == snoozeView) {
                    snoozeView.setImageResource(R.drawable.ic_lockscreen_snooze_activated);
                    helpText.setText(R.string.snooze);
                }
            }
            if (dragEvent.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                deactivateAll();
            }
            if (dragEvent.getAction() == DragEvent.ACTION_DROP) {
                if (view == resetView) {
                    reset();
                }
                if (view == discardView) {
                    discard();
                }
                if (view == snoozeView) {
                    snooze();
                }
                if (view == rootView) {
                    deactivateAll();
                }

                deactivateAll();
                dragEnd();
            }
            return true;
        }


        public void snooze() {

            ((MainActivity)getActivity()).killAutoReset();

            ((MainActivity)getActivity()).sendCancelIntent();

            MainActivity.ringtoneManager.stopPreviousRingtone();

            Intent resetIntent = new Intent(getActivity(), RandomAlarmService.class);
            resetIntent.setAction(RandomAlarmService.snooze);
            getActivity().startService(resetIntent);
            Toast.makeText(getActivity(), getString(R.string.snoozetoast), Toast.LENGTH_LONG).show();
        }

        public void reset() {
            ((MainActivity)getActivity()).killAutoReset();
            ((MainActivity)getActivity()).sendCancelIntent();

            MainActivity.ringtoneManager.stopPreviousRingtone();

            Toast.makeText(getActivity(), getString(R.string.resettoast), Toast.LENGTH_LONG).show();

            Intent resetIntent = new Intent(getActivity(), RandomAlarmService.class);
            resetIntent.setAction(RandomAlarmService.nextAlarm);
            getActivity().startService(resetIntent);
        }

        public void discard() {

            ((MainActivity)getActivity()).killAutoReset();
            ((MainActivity)getActivity()).sendCancelIntent();
            MainActivity.ringtoneManager.stopPreviousRingtone();

            SharedPreferences.Editor editor =  PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
            editor.putBoolean("pref_enabled", false);
            editor.commit();
            Intent intent = new Intent(getActivity(), RandomAlarmService.class);
            intent.setAction(RandomAlarmService.cancelNext);
            getActivity().startService(intent);
            Toast.makeText(getActivity(), getString(R.string.discardtoast), Toast.LENGTH_LONG).show();
        }

        public void deactivateAll() {
            resetView.setImageResource(R.drawable.ic_lap_normal);
            discardView.setImageResource(R.drawable.ic_lockscreen_discard_normal);
            snoozeView.setImageResource(R.drawable.ic_lockscreen_snooze_normal);
            if (ringing) {
                alarmIconView.startAnimation(alarmAnimation);
            } else {
                alarmIconView.clearAnimation();
                alarmIconView.setVisibility(View.VISIBLE);
            }
            helpText.setText("");
        }

        public void ring() {
            Log.d(tag, "SliderFragment ringing");
            ringing = true;
            alarmIconView.startAnimation(alarmAnimation);
            snoozeView.setVisibility(View.VISIBLE);

        }

        public void cancelRing() {

            Log.d(tag, "SliderFragment cancelRinging");

            ringing = false;
            alarmIconView.clearAnimation();
            deactivateAll();
            snoozeView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            RingEventReceiver.ringerList.remove(this);
        }
    }

}
