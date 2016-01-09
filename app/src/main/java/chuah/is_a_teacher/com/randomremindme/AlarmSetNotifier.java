package chuah.is_a_teacher.com.randomremindme;

import java.util.ArrayList;

/**
 * Created by jychuah on 10/12/14.
 */
public class AlarmSetNotifier {

    private static ArrayList<AlarmSetListener> listeners = new ArrayList<AlarmSetListener>();

    public static void notifyListeners(long alarmTime) {
        for (AlarmSetListener listener : listeners) {
            if (listener != null) {
                listener.alarmSet(alarmTime);
            }
        }
    }

    public static void add(AlarmSetListener l) {
        listeners.add(l);
    }

    public static void remove(AlarmSetListener l) {
        listeners.remove(l);
    }

    public interface AlarmSetListener {
        public void alarmSet(long alarmTime);
    }
}
