package ug.air.switchaccess;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Created by jq on 1/21/18.
 */

public class AutoSummaryListPreference extends ListPreference {
    public AutoSummaryListPreference(Context context) {
        this(context, null);
    }

    public AutoSummaryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            setSummary(getSummary());
        }
    }

    @Override
    public CharSequence getSummary() {
        int pos = findIndexOfValue(getValue());
        try{
            return getEntries()[pos];
        }catch (Exception e){
            e.printStackTrace();
        }
        return super.getSummary();
    }
}
