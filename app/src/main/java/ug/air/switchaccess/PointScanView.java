package ug.air.switchaccess;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by jq on 8/30/17.
 */

public class PointScanView extends ScanView {

    Path mPathV;
    Path mPathH;
    Paint mPaintLineCrosshairsHoriz;
    Paint mPaintLineCrosshairsVert;
    Paint mPaintLineNodes;
    Paint mPaintCircle;
    double xPos;
    double yPos;
    double yPosDraw;
    int mScreenWidth;
    int mScreenHeight;
    int mDirection;
    int mYOffset;
    int sequenceStep;
    int mNumCycles;
    int mStepTime;

    SharedPreferences prefs;

    int mStrokeWidthCrosshairs = 4;
    int mStrokeWidthNodes = 2;
    int mFrameDelay = 25;
    int mMaxCycles = 2;
    double mCrossHairsAlpha = 1.0;
    boolean autoScan = true;

    AccessibilityNodeInfo mCurrentSelectedNode = null;

    private static final int DIRECTION_LEFT_TO_RIGHT = 1;
    private static final int DIRECTION_RIGHT_TO_LEFT = 2;
    private static final int DIRECTION_TOP_TO_BOTTOM = 3;
    private static final int DIRECTION_BOTTOM_TO_TOP = 4;
    private static final int DIRECTION_STOPPED = 5;

    private static final int SEQUENCE_STANDBY = 1;
    private static final int SEQUENCE_SELECTING_HORIZ = 2;
    private static final int SEQUENCE_SELECTING_VERT = 3;
    private static final int SEQUENCE_FINISHED = 4;

    private static final String TAG = "PointScanView";
    private Handler mHandler;

    Runnable mScanHandler = new Runnable() {
        @Override
        public void run() {
            try {
                updateCursorPosition();
                invalidate();
            } finally {
                if (sequenceStep==SEQUENCE_SELECTING_HORIZ || sequenceStep==SEQUENCE_SELECTING_VERT) {
                    mHandler.postDelayed(mScanHandler, mStepTime);
                }
            }
        }
    };

    void startScanning() {
        mScanHandler.run();
    }

    void stopScanning() {
        mHandler.removeCallbacks(mScanHandler);
    }

    public ScanAction switchPressed(int switchType) {
        reloadSettings();

        if (switchType==SWITCH_TYPE_SELECT) {

            switch (sequenceStep) {
                case SEQUENCE_STANDBY:
                    mNumCycles = 0;
                    mCurrentSelectedNode = null;
                    xPos = 0;
                    yPos = 0;
                    mCrossHairsAlpha = 1.0;
                    mDirection = DIRECTION_TOP_TO_BOTTOM;
                    sequenceStep = SEQUENCE_SELECTING_VERT;
                    if (autoScan) {
                        stopScanning();
                        startScanning();
                    }
                    else {
                        invalidate();
                    }
                    break;
                case SEQUENCE_SELECTING_VERT:
                    mNumCycles = 0;
                    mDirection = DIRECTION_LEFT_TO_RIGHT;
                    sequenceStep = SEQUENCE_SELECTING_HORIZ;
                    if (autoScan) {
                        stopScanning();
                        startScanning();
                    }
                    else {
                        updateCursorPosition();
                        invalidate();
                    }
                    break;
                case SEQUENCE_SELECTING_HORIZ:
                    mDirection = DIRECTION_STOPPED;
                    sequenceStep = SEQUENCE_FINISHED;
                    if (autoScan) stopScanning();
                    invalidate();
            }

            if (sequenceStep == SEQUENCE_FINISHED || (sequenceStep == SEQUENCE_STANDBY && mCurrentSelectedNode != null)) {

                return new ScanAction(ScanAction.ACTION_CLICK,
                        (int) Math.round(xPos), (int) Math.round(yPos), mCurrentSelectedNode);
            }
        }

        else if (switchType==SWITCH_TYPE_NEXT) {
            if (sequenceStep==SEQUENCE_STANDBY) {
                mNumCycles = 0;
                xPos = 0;
                yPos = 0;
                mCrossHairsAlpha = 1.0;
                mDirection = DIRECTION_TOP_TO_BOTTOM;
                sequenceStep = SEQUENCE_SELECTING_VERT;
            }

            updateCursorPosition();
            invalidate();
        }

        return new ScanAction(ScanAction.ACTION_NONE);
    }

    public PointScanView(Context context)
    {
        super(context);
        init(context);
    }

    public PointScanView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public PointScanView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    public void reloadSettings() {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getContext());
        mStrokeWidthCrosshairs = SP.getInt("crosshairs_line_thickness",2);
        mMaxCycles = SP.getInt("max_scan_cycles",2);
        autoScan = SP.getString("screen_scan_type", "auto").equals("auto");
        double x = (double) 100-prefs.getInt("keyboardScanSpeed",40);
        mStepTime = (int) (300. + .46*Math.pow(x,2) - 9.*x);
    }

    private void init(Context context)
    {
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = context.getResources().getDisplayMetrics().heightPixels;

        prefs = context.getSharedPreferences(SwitchboardPreferences.GENERAL_SETTINGS_FILE_KEY, MODE_PRIVATE);

        mHandler = new Handler();

        resetCrosshairs();

        mPathH = new Path();
        mPathV = new Path();

        float strokeWidth = mStrokeWidthCrosshairs *getResources().getDisplayMetrics().density;

        int[] gradientColors = {Color.parseColor("#666666"),
                Color.parseColor("#046380"),
                Color.parseColor("#666666")};
        float[] gradientPositions = {(float)0.2,(float)0.6,(float)0.2};

        mPaintLineCrosshairsHoriz = new Paint();
        mPaintLineCrosshairsHoriz.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintLineCrosshairsHoriz.setShader(new LinearGradient(0, 0, 0, strokeWidth*2,
                gradientColors, gradientPositions, Shader.TileMode.REPEAT));

        mPaintLineCrosshairsVert = new Paint();
        mPaintLineCrosshairsVert.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintLineCrosshairsVert.setShader(new LinearGradient(0, 0, strokeWidth*2, 0,
                gradientColors, gradientPositions, Shader.TileMode.REPEAT));

        mPaintLineNodes = new Paint();
        mPaintLineNodes.setColor(Color.parseColor("#046380"));
        mPaintLineNodes.setStrokeWidth(mStrokeWidthNodes *getResources().getDisplayMetrics().density);
        mPaintLineNodes.setStyle(Paint.Style.STROKE);

        mPaintCircle = new Paint();
        mPaintCircle.setColor(Color.parseColor("#ff0000"));
        mPaintCircle.setStyle(Paint.Style.FILL);

        reloadSettings();
    }

    private void resetCrosshairs() {
        sequenceStep = SEQUENCE_STANDBY;
        mDirection = DIRECTION_STOPPED;
        mNumCycles = 0;
        xPos = 0;
        yPos = 0;
        if (autoScan) stopScanning();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
    }


    private void drawNodes(AccessibilityNodeInfo node, Canvas c) {

        if (node == null) return;

        mYOffset = mScreenHeight - c.getHeight();

        // Draw all the nodes
        if (node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
                && node.isVisibleToUser()) {
            Rect nodeBounds = new Rect();
            node.getBoundsInScreen(nodeBounds);

            if ((sequenceStep == SEQUENCE_SELECTING_HORIZ && nodeBounds.top <= yPos
                    && nodeBounds.bottom >= yPos)
                    || (sequenceStep == SEQUENCE_SELECTING_VERT)) {

                mPaintLineNodes.setColor(Color.parseColor("#66aaaaaa"));

                // The back, home and window list buttons are right on the bottom pixel, so offset
                int bottomOfScreenOffset = 0;
                if (nodeBounds.top==mScreenHeight) {
                    bottomOfScreenOffset = 10;
                }

                mPathH.reset();
                mPathH.moveTo(nodeBounds.left, nodeBounds.top - mYOffset - bottomOfScreenOffset);
                mPathH.lineTo(nodeBounds.right, nodeBounds.top - mYOffset - bottomOfScreenOffset);
                mPathH.lineTo(nodeBounds.right, nodeBounds.bottom - mYOffset);
                mPathH.lineTo(nodeBounds.left, nodeBounds.bottom - mYOffset);
                mPathH.lineTo(nodeBounds.left, nodeBounds.top - mYOffset - bottomOfScreenOffset);
                c.drawPath(mPathH, mPaintLineNodes);
            }
        }

        if (node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK) &&
                node.isVisibleToUser()) {
            Rect nodeBounds = new Rect();
            node.getBoundsInScreen(nodeBounds);

            if ((sequenceStep == SEQUENCE_SELECTING_HORIZ && mCurrentSelectedNode!=null
                    && mCurrentSelectedNode.hashCode()==node.hashCode())
                    || (sequenceStep==SEQUENCE_SELECTING_VERT && nodeBounds.top<=yPos
                    && nodeBounds.bottom>=yPos)) {

                mPaintLineNodes.setColor(Color.parseColor("#ff24a0c3"));

                // The back, home and window list buttons are right on the bottom pixel, so offset
                int bottomOfScreenOffset = 0;
                if (nodeBounds.top==mScreenHeight) {
                    bottomOfScreenOffset = 10;
                }

                mPathH.reset();
                mPathH.moveTo(nodeBounds.left, nodeBounds.top - mYOffset - bottomOfScreenOffset);
                mPathH.lineTo(nodeBounds.right, nodeBounds.top - mYOffset - bottomOfScreenOffset);
                mPathH.lineTo(nodeBounds.right, nodeBounds.bottom - mYOffset);
                mPathH.lineTo(nodeBounds.left, nodeBounds.bottom - mYOffset);
                mPathH.lineTo(nodeBounds.left, nodeBounds.top - mYOffset - bottomOfScreenOffset);
                c.drawPath(mPathH, mPaintLineNodes);
            }
        }

        // Propagate calls to children
        final int child_count = node.getChildCount();
        for (int i = 0; i < child_count; i++) {
            drawNodes(node.getChild(i), c);
        }
    }

    private void drawClickableNodes(Canvas c) {
        for (AccessibilityWindowInfo awi : getAccessibilityWindowInfo()) {
            AccessibilityNodeInfo node = awi.getRoot();
            drawNodes(node,c);
        }
    }

    private class NodeVerticalComparator implements Comparator<AccessibilityNodeInfo> {
        public int compare(AccessibilityNodeInfo node1, AccessibilityNodeInfo node2) {
            Rect node1Bounds = new Rect();
            node1.getBoundsInScreen(node1Bounds);
            Rect node2Bounds = new Rect();
            node2.getBoundsInScreen(node2Bounds);

            if (node1Bounds.top>node2Bounds.top) {
                return 1;
            }
            else if (node1Bounds.top<node2Bounds.top) {
                return -1;
            }
            else return 0;
        }
    }

    private class NodeHorizontalMeanComparator implements Comparator<AccessibilityNodeInfo> {
        public int compare(AccessibilityNodeInfo node1, AccessibilityNodeInfo node2) {
            Rect node1Bounds = new Rect();
            node1.getBoundsInScreen(node1Bounds);
            Rect node2Bounds = new Rect();
            node2.getBoundsInScreen(node2Bounds);

            if ((node1Bounds.left+node1Bounds.right)>(node2Bounds.left+node2Bounds.right)) {
                return 1;
            }
            else if ((node1Bounds.left+node1Bounds.right)<(node2Bounds.left+node2Bounds.right)) {
                return -1;
            }
            else return 0;
        }
    }

    private List<AccessibilityNodeInfo> enumerateNodes(AccessibilityNodeInfo root) {
        if (root==null) return null;

        List<AccessibilityNodeInfo> nodes = new ArrayList<>();

        if (root.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
                && root.isVisibleToUser()) {
            nodes.add(root);
        }

        final int child_count = root.getChildCount();
        for (int i = 0; i < child_count; i++) {
            List<AccessibilityNodeInfo> childNodes = enumerateNodes(root.getChild(i));
            if (childNodes!=null) {
                nodes.addAll(childNodes);
            }
        }

        return nodes;
    }

    private void updateCursorPosition() {

        NodeVerticalComparator verticalComparator = new NodeVerticalComparator();
        NodeHorizontalMeanComparator horizontalMeanComparator = new NodeHorizontalMeanComparator();

        ((SwitchAccessibilityService) getContext()).updateWindowList();

        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        for (AccessibilityWindowInfo awi : getAccessibilityWindowInfo()) {
            AccessibilityNodeInfo root = awi.getRoot();
            if (root != null) {
                allNodes.addAll(enumerateNodes(root));
            }
        }

        // If we have reached the bottom, then immediately move to the top again.
        if (mDirection == DIRECTION_BOTTOM_TO_TOP) {
            yPos = 0;
            mNumCycles += 1;
            mDirection = DIRECTION_TOP_TO_BOTTOM;

            // Check we didn't already exceed the max number of cycles in this direction.
            if (mNumCycles >= mMaxCycles) {
                resetCrosshairs();
                mPointScanButtonsView.setVisibility(View.GONE);
            }
        }

        // Find the next position downwards.
        if (mDirection == DIRECTION_TOP_TO_BOTTOM) {

            // Look for the next node along in the current direction from where we are now.
            Collections.sort(allNodes, verticalComparator);
            int first_node_index = -1;
            int top_boundary_first_node = -1;
            int bottom_boundary_first_node = -1;

            for (int i = 0; i < allNodes.size(); i++) {
                Rect nodeBounds = new Rect();
                allNodes.get(i).getBoundsInScreen(nodeBounds);
                if (nodeBounds.top > yPos) {
                    top_boundary_first_node = nodeBounds.top;
                    bottom_boundary_first_node = nodeBounds.bottom;
                    first_node_index = i;
                    break;
                }
            }

            // If we didn't find another node, then there's no more to see in this direction.
            if (first_node_index == -1) {
                mDirection = DIRECTION_BOTTOM_TO_TOP;
            }
            else {
                // Otherwise narrow down the available area by looking at any nodes which are
                // overlapping in this direction.
                int max_top_boundary = top_boundary_first_node;
                int min_bottom_boundary = bottom_boundary_first_node;
                for (int i = first_node_index; i < allNodes.size(); i++) {
                    Rect nodeBounds = new Rect();
                    allNodes.get(i).getBoundsInScreen(nodeBounds);

                    if (nodeBounds.top > max_top_boundary && nodeBounds.top < min_bottom_boundary) {
                        max_top_boundary = nodeBounds.top;
                    }
                    if (nodeBounds.bottom < min_bottom_boundary && nodeBounds.bottom > max_top_boundary) {
                        min_bottom_boundary = nodeBounds.bottom;
                    }
                }

                // Check if there's anything more to come in this direction. If not, then change
                // direction for the next step.
                boolean moreToCome = false;
                for (int i = first_node_index; i < allNodes.size(); i++) {
                    Rect nodeBounds = new Rect();
                    allNodes.get(i).getBoundsInScreen(nodeBounds);
                    if (nodeBounds.top > max_top_boundary) {
                        moreToCome = true;
                    }
                }
                if (!moreToCome) {
                    mDirection = DIRECTION_BOTTOM_TO_TOP;
                }

                yPos = ((max_top_boundary + min_bottom_boundary) / 2);
            }
        }

        // If we have reached the right, then immediately move to the left again.
        if (mDirection == DIRECTION_RIGHT_TO_LEFT) {
            xPos = 0;
            mCurrentSelectedNode = null;
            mNumCycles += 1;
            mDirection = DIRECTION_LEFT_TO_RIGHT;

            // Check we didn't already exceed the max number of cycles in this direction.
            if (mNumCycles >= mMaxCycles) {
                resetCrosshairs();
                mPointScanButtonsView.setVisibility(View.GONE);
            }
        }

        if (mDirection==DIRECTION_LEFT_TO_RIGHT) {
            Collections.sort(allNodes, horizontalMeanComparator);

            int nodeXCentre;
            int selected_node_index = -1;

            // Find the next node along from where we are now
            for (int i = 0; i < allNodes.size(); i++) {
                Rect nodeBounds = new Rect();
                allNodes.get(i).getBoundsInScreen(nodeBounds);
                if (nodeBounds.top <= yPos && nodeBounds.bottom >= yPos) {
                    nodeXCentre = (nodeBounds.left + nodeBounds.right) / 2;
                    if (nodeXCentre > xPos && allNodes.get(i) != mCurrentSelectedNode) {
                        mCurrentSelectedNode = allNodes.get(i);
                        xPos = nodeXCentre;
                        selected_node_index = i;
                        break;
                    }
                }
            }

            // If we didn't find anything, then don't change anything.
            if (selected_node_index==-1 || selected_node_index==allNodes.size()-1) {
                mDirection=DIRECTION_RIGHT_TO_LEFT;
            }
            else {
                // Check if there's anything coming up
                boolean moreToCome = false;
                for (int i = selected_node_index+1; i < allNodes.size(); i++) {
                    Rect nodeBounds = new Rect();
                    allNodes.get(i).getBoundsInScreen(nodeBounds);
                    nodeXCentre = (nodeBounds.left + nodeBounds.right) / 2;
                    if (nodeXCentre >= xPos && nodeBounds.top <= yPos && nodeBounds.bottom >= yPos
                            && allNodes.get(i).hashCode() != mCurrentSelectedNode.hashCode()) {
                        moreToCome = true;
                    }
                }

                if (!moreToCome) {
                    // If there was only ever one possible choice, then activate it already to save
                    // one switch press.
                    int numPossibleChoices = 0;
                    for (int i = 0; i < allNodes.size(); i++) {
                        Rect nodeBounds = new Rect();
                        allNodes.get(i).getBoundsInScreen(nodeBounds);
                        if (nodeBounds.top <= yPos && nodeBounds.bottom >= yPos) {
                            mCurrentSelectedNode = allNodes.get(i);
                            numPossibleChoices++;
                        }
                    }

                    if (numPossibleChoices==1) {
                        mDirection = DIRECTION_STOPPED;
                        sequenceStep = SEQUENCE_FINISHED;
                    }
                    else {
                        // Otherwise if there's nothing else and there would be other options to
                        // choose from, then reset in the horizontal direction.
                        mDirection = DIRECTION_RIGHT_TO_LEFT;
                    }
                }
            }
        }
    }

    @Override
    public void onDraw(Canvas c)
    {
        super.onDraw(c);

        mYOffset = mScreenHeight - c.getHeight();

        if (sequenceStep==SEQUENCE_STANDBY) {
            return;
        }

        if (sequenceStep==SEQUENCE_SELECTING_VERT || sequenceStep==SEQUENCE_SELECTING_HORIZ) {

            // In case the screen size changed (e.g. rotation) and we're outside the drawable area
            xPos = Math.min(xPos, mScreenWidth);
            yPos = Math.min(yPos, mScreenHeight);

            // If yPos is at the bottom of the screen, draw the line slightly higher so that
            // it is visible.
            yPosDraw = Math.min(yPos, mScreenHeight - 2*mStrokeWidthCrosshairs);

            // Now draw on the areas of clickable nodes
            drawClickableNodes(c);

            // Draw the crosshairs
            mPathV.reset();
            mPathV.addRect(0,(float) yPosDraw - mYOffset-mStrokeWidthCrosshairs,
                    (float) mScreenWidth,(float) yPosDraw - mYOffset+mStrokeWidthCrosshairs, Path.Direction.CW);
            c.drawPath(mPathV, mPaintLineCrosshairsHoriz);

            if (sequenceStep == SEQUENCE_SELECTING_HORIZ) {
                mPathH.reset();
                mPathH.addRect((float) xPos-mStrokeWidthCrosshairs,0,
                        (float) xPos+mStrokeWidthCrosshairs, mScreenHeight, Path.Direction.CW);
                c.drawPath(mPathH, mPaintLineCrosshairsVert);
            }
        }

        else if (sequenceStep==SEQUENCE_FINISHED) {

            mCrossHairsAlpha -= .05;

            if (mCrossHairsAlpha < .3) {
                resetCrosshairs();
            }
            else {
                String alphaString = Integer.toHexString((int) Math.round(256 * mCrossHairsAlpha));
                if (alphaString.length() == 1) {
                    alphaString = "0" + alphaString;
                }
                mPaintCircle.setColor(Color.parseColor("#" + alphaString + "ff0000"));
                c.drawCircle((int) Math.round(xPos), (int) Math.round(yPos - mYOffset),
                        12 * getResources().getDisplayMetrics().density, mPaintCircle);
                postInvalidateDelayed(mFrameDelay);
            }
        }
    }
}
