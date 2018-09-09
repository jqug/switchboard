package ug.air.switchaccess;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Created by jq on 9/7/17.
 */

public class ScanAction {
    public static final int ACTION_NONE = 0;
    public static final int ACTION_CLICK = 1;
    public static final int ACTION_TIMEOUT = 2;

    public int actionType;
    public int xPos;
    public int yPos;
    public AccessibilityNodeInfo node;

    public ScanAction(int type) {
        this.actionType = type;
    }

    public ScanAction(AccessibilityNodeInfo node) {
        this.actionType = ACTION_CLICK;
        this.xPos = -1;
        this.yPos = -1;
        this.node = node;
    }

    public ScanAction(int type, int x, int y, AccessibilityNodeInfo node) {
        this.actionType = type;
        this.xPos = x;
        this.yPos = y;
        this.node = node;
    }
}
