package chuah.is_a_teacher.com.randomremindme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class RingEventReceiver extends BroadcastReceiver {
    public static String ring = "chuah.is_a_teacher.com.randomremindme.RingEventReceiver.ring";
    public static String cancelRing = "chuah.is_a_teacher.com.randomremindme.RingEventReceiver.cancelRing";

    public static List<Ringer> ringerList = new ArrayList<Ringer>();

    public RingEventReceiver() {

    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ring)) {
            for (Ringer r : ringerList) {
                r.ring();
            }
        }
        if (intent.getAction().equals(cancelRing)) {
            for (Ringer r : ringerList) {
                r.cancelRing();
            }
        }

    }
}
