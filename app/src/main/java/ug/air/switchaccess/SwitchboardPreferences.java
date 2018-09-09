package ug.air.switchaccess;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by jq on 9/13/17.
 */

public class SwitchboardPreferences {

    public static final String GENERAL_SETTINGS_FILE_KEY = "ug.air.switchacess.GENERAL_SETTINGS_KEY";
    public static final String SWITCH_CODES_FILE_KEY = "ug.air.switchacess.SWITCH_CODES_KEY";

    public static final int NUM_SWITCH_TYPES = 6;
    public static final String SWITCH_TYPE_SELECTITEM = "switchCode";
    public static final String SWITCH_TYPE_NEXTITEM = "switchCodeNext";
    public static final String SWITCH_TYPE_HOME = "switchCodeHome";
    public static final String SWITCH_TYPE_BACK = "switchCodeBack";
    public static final String SWITCH_TYPE_SCROLLDOWN = "switchCodeScrollDown";
    public static final String SWITCH_TYPE_SCROLLUP = "switchCodeScrollUp";
    public static final String SWITCH_TYPE_UNDEFINED = "undefined";

    public static final String PREFS_SCAN_SPEED = "keyboardScanSpeed";

    public static int[] getAllAssignedSwitchCodes(Context mContext) {
        int[] assignedCodes = {};
        for (int idx=0;idx<NUM_SWITCH_TYPES;idx++) {
            assignedCodes = combine(assignedCodes, getSwitchCodes(mContext, switchTypeFromIndex(idx)));
        }
        return assignedCodes;
    }

    public static int[] getSwitchCodes(Context mContext, String switchType) {
        SharedPreferences prefs = mContext.getSharedPreferences(SwitchboardPreferences.SWITCH_CODES_FILE_KEY, 0);
        int size = prefs.getInt(switchType + "_size", 0);
        int array[] = new int[size];
        for(int i=0;i<size;i++)
            array[i] = prefs.getInt(switchType + "_" + i,-1);
        return array;
    }

    public static boolean setSwitchCodes(int[] array, Context mContext, String switchType) {
        SharedPreferences prefs = mContext.getSharedPreferences(SwitchboardPreferences.SWITCH_CODES_FILE_KEY, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(switchType +"_size", array.length);
        for(int i=0;i<array.length;i++)
            editor.putInt(switchType + "_" + i, array[i]);
        return editor.commit();
    }

    public static String nextSwitchTypeToAssign(Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences(SwitchboardPreferences.SWITCH_CODES_FILE_KEY, 0);
        // Find the lowest switch type index that doesn't yet have any switch codes assigned
        for (int idx=0;idx<NUM_SWITCH_TYPES;idx++) {
            if (prefs.getInt(switchTypeFromIndex(idx) + "_size", 0)==0) return switchTypeFromIndex(idx);
        }
        // If all switch types have already been assigned, then default to SELECT_ITEM type
        return SWITCH_TYPE_SELECTITEM;
    }

    public static String switchTypeFromIndex(int switchTypeIdx) {
        String switchType;
        switch(switchTypeIdx) {
            case 0: switchType = SwitchboardPreferences.SWITCH_TYPE_SELECTITEM;
                break;
            case 1: switchType = SwitchboardPreferences.SWITCH_TYPE_NEXTITEM;
                break;
            case 2: switchType = SwitchboardPreferences.SWITCH_TYPE_HOME;
                break;
            case 3: switchType = SwitchboardPreferences.SWITCH_TYPE_BACK;
                break;
            case 4: switchType = SwitchboardPreferences.SWITCH_TYPE_SCROLLDOWN;
                break;
            case 5: switchType = SwitchboardPreferences.SWITCH_TYPE_SCROLLUP;
                break;
            default: switchType = "UNKNOWN";
                break;
        }
        return switchType;
    }

    private static int count(final int[] array, final int v) {
        int c = 0;
        for (final int e : array)
            if (e == v)
                c += 1;
        return c;
    }

    private static int[] combine(int[] a, int[] b){
        int length = a.length + b.length;
        int[] result = new int[length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static void unassignSwitchCode(Context mContext,int switchCode) {
        for (int idx=0;idx<NUM_SWITCH_TYPES;idx++) {
            int [] assignedCodes = getSwitchCodes(mContext,switchTypeFromIndex(idx));
            int numOccurrences = count(assignedCodes,switchCode);
            if (numOccurrences>0) {
                int [] newAssignedCodes = new int[assignedCodes.length-numOccurrences];
                int j = 0;
                for (final int e : assignedCodes) {
                    if (e != switchCode) {
                        newAssignedCodes[j] = e;
                        j += 1;
                    }
                }
                setSwitchCodes(newAssignedCodes,mContext,switchTypeFromIndex(idx));
            }
        }
    }
}
