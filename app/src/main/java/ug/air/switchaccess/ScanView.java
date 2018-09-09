package ug.air.switchaccess;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jq on 1/12/18.
 */

public class ScanView extends View {

    private int mScreenWidth;
    private int mScreenHeight;
    public View mPointScanButtonsView;
    private List<AccessibilityWindowInfo> mWindowInfo = new ArrayList<>();

    public static final int SWITCH_TYPE_SELECT = 1;
    public static final int SWITCH_TYPE_NEXT = 2;

    public ScanView(Context context) {
        super(context);
    }

    public ScanView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ScanView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public void reloadSettings() {
    }

    public ScanAction switchPressed(int switchType) {
        return null;
    }

    public void setAccessibilityWindowInfo(List<AccessibilityWindowInfo> windowInfo) {
        mWindowInfo = windowInfo;
    }

    public List<AccessibilityWindowInfo> getAccessibilityWindowInfo() {
        return mWindowInfo;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
    }
}
