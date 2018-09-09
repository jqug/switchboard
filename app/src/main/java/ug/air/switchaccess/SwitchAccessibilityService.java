package ug.air.switchaccess;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SwitchAccessibilityService extends AccessibilityService {
    private ScanView mScanView;
    private View mPointScanButtonsView;
    private SharedPreferences mSharedPrefs;
    private boolean keyboardVisible = false;
    InputMethodManager mIMEMgr;
    WindowManager mWindowManager;
    WindowManager.LayoutParams mLayoutParamsCrosshairs;
    WindowManager.LayoutParams mLayoutParamsButtons;

    private static final String TAG = "SwitchAccService";

    public static final String POINT_SCAN_SETTINGS_CHANGE = "PointScanSpeedChanged";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Check if the point scan settings have been changed
        if (event!=null && event.getEventType()==AccessibilityEvent.TYPE_ANNOUNCEMENT &&
                event.getClassName()!=null && event.getClassName().toString().contains(getPackageName()) &&
                event.getText()!=null && event.getText().toString().contains(POINT_SCAN_SETTINGS_CHANGE)) {
            if (mScanView !=null) {
                mScanView.reloadSettings();
                updateWindowList();
            }
        }
    }

    @Override
    public void onInterrupt() {}


    public void updateWindowList() {
        // Check whether an IME is visible
        keyboardVisible = false;

        List<AccessibilityWindowInfo> l = getWindows();
        for (AccessibilityWindowInfo awi : l) {
            if (awi != null) {
                if (awi.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    keyboardVisible = true;
                    break;
                }
            }
        }

        // Load the available accessibility nodes into the View
        // Update the list of windows and their accessibility nodes so that the clickable areas can be drawn on the screen
        mScanView.setAccessibilityWindowInfo(l);
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (!prefs.getBoolean("isServiceEnabled", true)){
            return false;
        }

        try {
            int[] mSelectItemSwitchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_SELECTITEM);
            int[] mNextItemSwitchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_NEXTITEM);
            int[] mBackSwitchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_BACK);
            int[] mHomeSwitchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_HOME);
            int[] mScrollUpSwitchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_SCROLLUP);
            int[] mScrollDownSwitchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_SCROLLDOWN);

            int keyCode = event.getKeyCode();
            String switchType = SwitchboardPreferences.SWITCH_TYPE_UNDEFINED;

            boolean ownKeyboardActive = false;

            // Was the selection switch pressed?
            if (mSelectItemSwitchCodes!=null) {
                for (int sc : mSelectItemSwitchCodes) {
                    if (keyCode==sc) {
                        switchType = SwitchboardPreferences.SWITCH_TYPE_SELECTITEM;
                    }
                }
            }

            // Or was the "next item" switch pressed?
            if (mNextItemSwitchCodes!=null) {
                for (int sc : mNextItemSwitchCodes) {
                    if (keyCode==sc) {
                        switchType = SwitchboardPreferences.SWITCH_TYPE_NEXTITEM;
                    }
                }
            }

            // Or was the "back" switch pressed?
            if (mBackSwitchCodes!=null) {
                for (int sc : mBackSwitchCodes) {
                    if (keyCode==sc) {
                        switchType = SwitchboardPreferences.SWITCH_TYPE_BACK;
                    }
                }
            }

            // Or was the "home" switch pressed?
            if (mHomeSwitchCodes!=null) {
                for (int sc : mHomeSwitchCodes) {
                    if (keyCode==sc) {
                        switchType = SwitchboardPreferences.SWITCH_TYPE_HOME;
                    }
                }
            }

            // Or was the "scroll up" switch pressed?
            if (mScrollUpSwitchCodes!=null) {
                for (int sc : mScrollUpSwitchCodes) {
                    if (keyCode==sc) {
                        switchType = SwitchboardPreferences.SWITCH_TYPE_SCROLLUP;
                    }
                }
            }

            // Or was the "scroll down" switch pressed?
            if (mScrollDownSwitchCodes!=null) {
                for (int sc : mScrollDownSwitchCodes) {
                    if (keyCode==sc) {
                        switchType = SwitchboardPreferences.SWITCH_TYPE_SCROLLDOWN;
                    }
                }
            }

            // Stop processing if it wasn't one of the registered switches that's been pressed,
            if (switchType.equals(SwitchboardPreferences.SWITCH_TYPE_UNDEFINED)) return false;


            // Swallow any auto-repeats from the switch
            if (event.getRepeatCount()>0) {
                return true;
            }

            updateWindowList();

            // If an IME is visible, check if it's our custom switch-enabled one
            if (keyboardVisible) {
                String pkgName = Settings.Secure.getString(getBaseContext().getContentResolver(),
                        Settings.Secure.DEFAULT_INPUT_METHOD);
                if (pkgName != null && pkgName.contains(this.getPackageName())) {
                    ownKeyboardActive = true;
                }
            }

            // If it's our own keyboard, then let it handle the switch press. Otherwise process the
            // switch press here.
            if (!ownKeyboardActive) {

                // Stop processing if we're currently looking at a window that doesn't have switch access.
                Set<String> emptySet = new HashSet<>();
                Set<String> suppressedSwitchApps = mSharedPrefs.getStringSet("suppressed_apps",emptySet);
                if (suppressedSwitchApps.size()>0) {
                    AccessibilityNodeInfo root = getRootInActiveWindow();
                    if (root != null) {
                        String packagename = getRootInActiveWindow().getPackageName().toString();
                        if (suppressedSwitchApps.contains(packagename)) {
                            return false;
                        }
                    }
                }

                if (event.getAction() == KeyEvent.ACTION_UP) {

                    if (switchType.equals(SwitchboardPreferences.SWITCH_TYPE_HOME)) {
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                    }
                    else if (switchType.equals(SwitchboardPreferences.SWITCH_TYPE_BACK)) {
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    }
                    else if (switchType.equals(SwitchboardPreferences.SWITCH_TYPE_SCROLLDOWN)) {
                        scrollDown();
                    }
                    else if (switchType.equals(SwitchboardPreferences.SWITCH_TYPE_SCROLLUP)) {
                        scrollUp();
                    }
                    else if (switchType.equals(SwitchboardPreferences.SWITCH_TYPE_SELECTITEM)) {
                        ScanAction action = mScanView.switchPressed(ScanView.SWITCH_TYPE_SELECT);
                        if (action.actionType == ScanAction.ACTION_CLICK) {
                            try {
                                action.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }
                            finally {
                                mPointScanButtonsView.setVisibility(View.GONE);
                            }
                        }
                        else {
                            showHelperButtons();
                        }
                    }
                    else if (switchType.equals(SwitchboardPreferences.SWITCH_TYPE_NEXTITEM)) {
                        showHelperButtons();
                        mScanView.switchPressed(ScanView.SWITCH_TYPE_NEXT);
                    }
                }
                return true;
            }
            return false;
        }
        catch (Exception e) {
            return false;
        }
    }

    private void showHelperButtons() {
        if (mSharedPrefs.getBoolean("show_helper_panel", true)) {
            setUpHelperPanelButtons();
            mPointScanButtonsView.setVisibility(View.VISIBLE);
            updateWindowList();
        }
    }

    @Override
    protected void onServiceConnected() {

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        mIMEMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS |
                AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY;
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mScanView = new PointScanView(this) {};

        mLayoutParamsButtons = new WindowManager.LayoutParams();
        mLayoutParamsButtons.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        mLayoutParamsButtons.format = PixelFormat.TRANSLUCENT;
        mLayoutParamsButtons.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParamsButtons.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParamsButtons.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParamsButtons.gravity = Gravity.TOP | Gravity.START;

        mPointScanButtonsView = LayoutInflater.from(this).inflate(R.layout.point_scan, null);

        setUpHelperPanelButtons();

        mWindowManager.addView(mPointScanButtonsView, mLayoutParamsButtons);

        mLayoutParamsCrosshairs = new WindowManager.LayoutParams();
        mLayoutParamsCrosshairs.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        mLayoutParamsCrosshairs.format = PixelFormat.TRANSLUCENT;
        mLayoutParamsCrosshairs.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        mLayoutParamsCrosshairs.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParamsCrosshairs.width = WindowManager.LayoutParams.MATCH_PARENT;
        mLayoutParamsCrosshairs.height = WindowManager.LayoutParams.MATCH_PARENT;
        mLayoutParamsCrosshairs.gravity = Gravity.TOP | Gravity.START;

        mWindowManager.addView(mScanView, mLayoutParamsCrosshairs);

        mPointScanButtonsView.setVisibility(View.INVISIBLE);
    }

    private void setUpHelperPanelButtons() {
        // Point scan view needs a reference to the helper buttons to make them invisible in case of timeout
        mScanView.mPointScanButtonsView = mPointScanButtonsView;

        // Which buttons has the user selected to be visible in the helper panel?
        Set<String> defaultSelection = new HashSet<>();
        defaultSelection.add("home");
        defaultSelection.add("back");
        defaultSelection.add("scroll");
        Set<String> helperPanelSelectedButtons = mSharedPrefs.getStringSet("helper_buttons_visible",defaultSelection);

        if (helperPanelSelectedButtons.contains("back")) {
            mPointScanButtonsView.findViewById(R.id.backSoftButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                }
            });
            mPointScanButtonsView.findViewById(R.id.backSoftButton).setVisibility(View.VISIBLE);
        }
        else {
            mPointScanButtonsView.findViewById(R.id.backSoftButton).setVisibility(View.GONE);
        }

        if (helperPanelSelectedButtons.contains("home")) {
            mPointScanButtonsView.findViewById(R.id.homeButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                }
            });
            mPointScanButtonsView.findViewById(R.id.homeButton).setVisibility(View.VISIBLE);
        }
        else {
            mPointScanButtonsView.findViewById(R.id.homeButton).setVisibility(View.GONE);
        }

        if (helperPanelSelectedButtons.contains("notifications")) {
            mPointScanButtonsView.findViewById(R.id.notificationsButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                }
            });
            mPointScanButtonsView.findViewById(R.id.notificationsButton).setVisibility(View.VISIBLE);
        }
        else {
            mPointScanButtonsView.findViewById(R.id.notificationsButton).setVisibility(View.GONE);
        }

        if (helperPanelSelectedButtons.contains("scroll")) {
            configureScrollButton();
            mPointScanButtonsView.findViewById(R.id.scrollDownButton).setVisibility(View.VISIBLE);
            mPointScanButtonsView.findViewById(R.id.scrollUpButton).setVisibility(View.VISIBLE);
            mPointScanButtonsView.findViewById(R.id.scrollLabel).setVisibility(View.VISIBLE);
        }
        else {
            mPointScanButtonsView.findViewById(R.id.scrollDownButton).setVisibility(View.GONE);
            mPointScanButtonsView.findViewById(R.id.scrollUpButton).setVisibility(View.GONE);
            mPointScanButtonsView.findViewById(R.id.scrollLabel).setVisibility(View.GONE);
        }
    }

    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo root, AccessibilityNodeInfo.AccessibilityAction action) {
        if (root==null) {
            return null;
        }

        Deque<AccessibilityNodeInfo> deque = new ArrayDeque<>();
        deque.add(root);
        while (!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.removeFirst();
            if (node != null && node.getActionList() != null && node.getActionList().contains(action)) {
                return node;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i)!=null) {
                    deque.addLast(node.getChild(i));
                }
            }
        }
        return null;
    }

    private void configureScrollButton() {
        ImageView scrollDownButton = mPointScanButtonsView.findViewById(R.id.scrollDownButton);
        scrollDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccessibilityNodeInfo scrollableDown = findScrollableNode(getRootInActiveWindow(), AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                if (scrollableDown != null) {
                    scrollableDown.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.getId());
                }
            }
        });

        ImageView scrollUpButton = mPointScanButtonsView.findViewById(R.id.scrollUpButton);
        scrollUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccessibilityNodeInfo scrollableUp = findScrollableNode(getRootInActiveWindow(),AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
                if (scrollableUp != null) {
                    scrollableUp.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.getId());
                }
            }
        });
    }

    private void scrollDown() {
        AccessibilityNodeInfo scrollable = findScrollableNode(getRootInActiveWindow(), AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        if (scrollable != null) {
            scrollable.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.getId());
        }
    }

    private void scrollUp() {
        AccessibilityNodeInfo scrollable = findScrollableNode(getRootInActiveWindow(), AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        if (scrollable != null) {
            scrollable.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.getId());
        }
    }
}
