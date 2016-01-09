package chuah.is_a_teacher.com.randomremindme;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * https://github.com/commonsguy
 */
public class TimePreference extends DialogPreference {

    private long time = 0;
    private TimePicker picker = null;

    public TimePreference(Context ctxt) {
        this(ctxt, null);
    }

    public TimePreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, 0);
    }

    public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        return (picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        picker.setCurrentHour((int)(time / 1000 / 60 / 60));
        picker.setCurrentMinute((int)(time / 1000 / 60 % 60));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            time = picker.getCurrentHour() * 1000 * 60 * 60 + picker.getCurrentMinute() * 1000 * 60;

            setSummary(getSummary());

            if (callChangeListener(time)) {
                persistLong(time);
                notifyChanged();
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if (restoreValue) {
            if (defaultValue == null) {
                time = getPersistedLong(System.currentTimeMillis());
            } else {
                time = (Long.parseLong(getPersistedString((String) defaultValue)));
            }
        } else {
            if (defaultValue == null) {
                time = (System.currentTimeMillis());
            } else {
                time = (Long.parseLong((String) defaultValue));
            }
        }
        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {

        int h = (int)(time / 1000 / 60 / 60);
        int m = (int)(time / 1000 / 60 % 60);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);

        return DateFormat.getTimeFormat(getContext()).format(new Date(c.getTimeInMillis()));
    }
}