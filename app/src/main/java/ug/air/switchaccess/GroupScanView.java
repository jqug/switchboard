package ug.air.switchaccess;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by jq on 8/30/17.
 */

public class GroupScanView extends ScanView {


    Paint mPaintLineNodes;
    int mScreenWidth;
    int mScreenHeight;
    int sequenceStep;
    int mNumCycles;
    Path mPath;

    SharedPreferences prefs;

    int mStrokeWidthNodes = 3;

    int mStepTime = 1000;

    int mMaxCycles = 3;
    int mMinNodesPerGroup = 6;
    int mCurrentCycle = 0;
    int mNumGroups = 0;
    int mDefaultNumGroups = 2;
    int mCurrentSelectedGroup = -1;
    int mGroupingType;

    List<Rect> mCurrentBoxes;

    List<AccessibilityNodeInfo> mCandidateNodes;

    HashMap<Integer,Integer> nodeGroupAllocation;

    View mPointScanButtonsView;

    private static final int SEQUENCE_STANDBY = 1;
    private static final int SEQUENCE_SELECTING_BOX = 2;
    private static final int SEQUENCE_FINISHED = 4;

    private static final int GROUPING_TYPE_BOXES = 1;
    private static final int GROUPING_TYPE_PROBABILISTIC = 2;

    private boolean ignoreNextInvalidate = false;
    private int numInvalidationRequestsInQueue = 0;

    private static final String TAG = "GroupScanView";

    Paint paint;

    public ScanAction switchPressed() {

        if (sequenceStep==SEQUENCE_STANDBY) {
            sequenceStep = SEQUENCE_SELECTING_BOX;
            mCurrentCycle = 0;
            mCurrentSelectedGroup = -1;
            mCandidateNodes = null;
            allocateCurrentNodesToGroups();
        }
        else if (sequenceStep==SEQUENCE_SELECTING_BOX) {

            // refine the list of candidate nodes - narrow down to the current highlighted group
            List<AccessibilityNodeInfo> newCandidateNodes = new ArrayList<AccessibilityNodeInfo>();
            for (int i=0;i<mCandidateNodes.size();i++) {
                if (getGroupIdOfNode(mCandidateNodes.get(i).hashCode())==mCurrentSelectedGroup) {
                    newCandidateNodes.add(mCandidateNodes.get(i));
                }
            }
            mCandidateNodes = newCandidateNodes;

            //Log.d(TAG,"Selected group: " + mCurrentSelectedGroup + ", new candidate set size: " + mCandidateNodes.size());

            // if we've narrowed it down to one node, then we're finished
            if (mCandidateNodes.size()==1) {
                resetScan();
                return new ScanAction(mCandidateNodes.get(0));
            }

            // otherwise split up the remaining candidates into new groups
            allocateCurrentNodesToGroups();
            mCurrentCycle = 0;
            mCurrentSelectedGroup = -1;
        }

        numInvalidationRequestsInQueue += 1;
        invalidate();
        //ignoreNextInvalidate = true;

        return new ScanAction(ScanAction.ACTION_NONE);
    }

    public GroupScanView(Context context)
    {
        super(context);
        init(context);
    }

    public GroupScanView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public GroupScanView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    public void reloadSettings() {
        double x = (double) 100-prefs.getInt("keyboardScanSpeed",40);
        mStepTime = (int) (300. + .46*Math.pow(x,2) - 9.*x);
    }

    private void init(Context context)
    {
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = context.getResources().getDisplayMetrics().heightPixels;
        mGroupingType = GROUPING_TYPE_BOXES;

        prefs = context.getSharedPreferences(SwitchboardPreferences.GENERAL_SETTINGS_FILE_KEY, MODE_PRIVATE);

        resetScan();

        mPath = new Path();

        mPaintLineNodes = new Paint();
        mPaintLineNodes.setColor(Color.parseColor("#046380"));
        mPaintLineNodes.setStrokeWidth(mStrokeWidthNodes *getResources().getDisplayMetrics().density);
        mPaintLineNodes.setStyle(Paint.Style.STROKE);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mCurrentBoxes = new ArrayList<Rect>();
        mCurrentBoxes.add(new Rect(0,0,mScreenWidth,mScreenHeight));

        nodeGroupAllocation = new HashMap<>();

        mGroupingType = GROUPING_TYPE_BOXES;
    }

    private void resetScan() {
        sequenceStep = SEQUENCE_STANDBY;
        mNumCycles = 0;
        mCurrentSelectedGroup = -1;
        mCurrentCycle = -1;
        //if (nodeGroupAllocation!=null) nodeGroupAllocation.clear();
        //if (mCandidateNodes!=null) mCandidateNodes.clear();
        numInvalidationRequestsInQueue += 1;
        invalidate();
        //ignoreNextInvalidate = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
    }

    private int getGroupIdOfNode(int nodeHashCode) {
        if (nodeGroupAllocation.containsKey(nodeHashCode)) {
            return nodeGroupAllocation.get(nodeHashCode);
        }
        else {
            return -1;
        }
    }

    private List<AccessibilityNodeInfo> enumerateNodes(AccessibilityNodeInfo root) {
        if (root==null) return null;

        List<AccessibilityNodeInfo> nodes = new ArrayList<AccessibilityNodeInfo>();

        if (root.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK) && root.isVisibleToUser()) {
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

    private class VerticalPriorityComparator implements Comparator<AccessibilityNodeInfo> {
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
            else if (node1Bounds.left>node2Bounds.left) {
                return 1;
            }
            else if (node1Bounds.left<node2Bounds.left) {
                return -1;
            }
            else return 0;
        }
    }

    private class HorizontalPriorityComparator implements Comparator<AccessibilityNodeInfo> {
        public int compare(AccessibilityNodeInfo node1, AccessibilityNodeInfo node2) {
            Rect node1Bounds = new Rect();
            node1.getBoundsInScreen(node1Bounds);
            Rect node2Bounds = new Rect();
            node2.getBoundsInScreen(node2Bounds);

            if (node1Bounds.left>node2Bounds.left) {
                return 1;
            }
            else if (node1Bounds.left<node2Bounds.left) {
                return -1;
            }
            else if (node1Bounds.top>node2Bounds.top) {
                return 1;
            }
            else if (node1Bounds.top<node2Bounds.top) {
                return -1;
            }
            else return 0;
        }
    }

    private void allocateCurrentNodesToGroups() {

        nodeGroupAllocation.clear();

        VerticalPriorityComparator verticalComparator = new VerticalPriorityComparator();
        HorizontalPriorityComparator horizontalComparator = new HorizontalPriorityComparator();

        // if starting a new selection, get all the nodes currently available, otherwise carry on
        // from existing list.
        if (mCandidateNodes==null || mCandidateNodes.size()==0) {
            List<AccessibilityNodeInfo> allNodes = new ArrayList<AccessibilityNodeInfo>();
            for (AccessibilityWindowInfo awi : getAccessibilityWindowInfo()) {
                AccessibilityNodeInfo root = awi.getRoot();
                allNodes.addAll(enumerateNodes(root));
            }
            mCandidateNodes = allNodes;
        }

        Log.d(TAG,"Candidate node set size=" + mCandidateNodes.size());

        // if probabilistic grouping, then calculate relative scores
        if (mGroupingType==GROUPING_TYPE_PROBABILISTIC) {

        }

        else if (mGroupingType==GROUPING_TYPE_BOXES) {

            //Log.d(TAG,"Allocating groups.");

            // get the left/top coordinates of all nodes currently accessible
            int minX = mScreenWidth;
            int maxX = 0;
            int minY = mScreenHeight;
            int maxY = 0;

            for (int i = 0; i < mCandidateNodes.size(); i++) {
                Rect nodeBounds = new Rect();
                mCandidateNodes.get(i).getBoundsInScreen(nodeBounds);
                minX = Math.min(nodeBounds.left, minX);
                maxX = Math.max(nodeBounds.right, maxX);
                minY = Math.min(nodeBounds.top, minY);
                maxY = Math.max(nodeBounds.bottom, maxY);
            }

            //Log.d(TAG,"2. Candidate node set size=" + mCandidateNodes.size());

            // determine whether horizontal or vertical span is greater
            if ((maxX-minX)>(maxY-minY)) {
                //Log.d(TAG,"Horizontal precedence.");
                // sort with horizontal taking precedence
                mCandidateNodes.sort(horizontalComparator);
            }
            else {
                //Log.d(TAG,"Vertical precedence.");
                // sort with vertical taking precedence
                mCandidateNodes.sort(verticalComparator);
            }

            //Log.d(TAG,"3. Candidate node set size=" + mCandidateNodes.size());

            // if the number of available nodes is below threshold, put one node in each group.
            if (mCandidateNodes.size()<mMinNodesPerGroup) {
                for (int i = 0; i < mCandidateNodes.size(); i++) {
                    nodeGroupAllocation.put(mCandidateNodes.get(i).hashCode(),i);
                    //Log.d(TAG,"Allocating node " + mCandidateNodes.get(i).hashCode() + " to group " + i);
                }
                mNumGroups = mCandidateNodes.size();
            }
            else {
                for (int i = 0; i < mCandidateNodes.size(); i++) {
                    int allocatedGroup =  (int) Math.floor(((double)mDefaultNumGroups*i)/((double)1+mCandidateNodes.size()));
                    nodeGroupAllocation.put(mCandidateNodes.get(i).hashCode(),
                           allocatedGroup);
                    //Log.d(TAG,"Allocating node " + mCandidateNodes.get(i).hashCode() + " to group " + allocatedGroup);
                }
                mNumGroups = mDefaultNumGroups;
            }

            //Log.d(TAG,"5. Candidate node set size=" + mCandidateNodes.size());

            //Log.d(TAG,"Number of groups = " + mNumGroups);
        }
    }

    private void drawNodes(AccessibilityNodeInfo node, Canvas c) {

        if (node == null) return;

        //Log.d(TAG,"node: " + node);

        // Draw all the nodes
        if (node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK) && node.isVisibleToUser()) {
            Rect nodeBounds = new Rect();
            node.getBoundsInScreen(nodeBounds);
            int yOffset = c.getHeight()-mScreenHeight;
            nodeBounds.offset(0,yOffset);

            //Log.d(TAG,"curr sel group="+mCurrentSelectedGroup +", node group=" + getGroupIdOfNode(node.hashCode()));

            if (getGroupIdOfNode(node.hashCode())==mCurrentSelectedGroup) {

                //Draw transparent shape
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                c.drawRect(nodeBounds, paint);

                //mPaintLineNodes.setColor(Color.parseColor("#66aaaaaa"));

                // Depending on whether in current group or not
                //mPaintLineNodes.setColor(Color.parseColor("#ff24a0c3"));
                mPaintLineNodes.setColor(Color.parseColor("#ffffffff"));

                mPath.reset();
                mPath.moveTo(nodeBounds.left, nodeBounds.top);// - yOffset);
                mPath.lineTo(nodeBounds.right, nodeBounds.top);// - yOffset);
                mPath.lineTo(nodeBounds.right, nodeBounds.bottom);// - yOffset);
                mPath.lineTo(nodeBounds.left, nodeBounds.bottom);// - yOffset);
                mPath.lineTo(nodeBounds.left, nodeBounds.top);// - yOffset);
                c.drawPath(mPath, mPaintLineNodes);
            }
        }

        // propagate calls to children
        final int child_count = node.getChildCount();
        for (int i = 0; i < child_count; i++) {
            drawNodes(node.getChild(i), c);
        }
    }

    private void drawNodeGroups(Canvas c) {
        for (AccessibilityWindowInfo awi : getAccessibilityWindowInfo()) {
            //Log.d(TAG,"window: " + awi);
            AccessibilityNodeInfo root = awi.getRoot();
            drawNodes(root, c);
        }
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);

        numInvalidationRequestsInQueue -= 1;

        if (sequenceStep == SEQUENCE_STANDBY) {
            ignoreNextInvalidate = false;
            return;
        }
        else if (sequenceStep == SEQUENCE_SELECTING_BOX) {

            if (ignoreNextInvalidate) {
                Log.d(TAG, "Ignoring this redraw request.");
            } else {
                mCurrentSelectedGroup += 1;
                if (mCurrentSelectedGroup >= mNumGroups) {
                    mCurrentSelectedGroup = 0;
                    mCurrentCycle += 1;
                }
                if (mCurrentCycle >= mMaxCycles) {
                    resetScan();
                }
            }

            //Draw Overlay
            paint.reset();
            paint.setColor(Color.parseColor("#66000000"));
            paint.setStyle(Paint.Style.FILL);
            c.drawPaint(paint);

            drawNodeGroups(c);

            if (ignoreNextInvalidate) {
                ignoreNextInvalidate = false;
            }
            else {
                numInvalidationRequestsInQueue += 1;

                if (numInvalidationRequestsInQueue>1) ignoreNextInvalidate=true;

                postInvalidateDelayed(mStepTime);
            }
        }
        else if (sequenceStep==SEQUENCE_FINISHED) {
            // If needed, indicate what has been pressed. Highlight the selected node?
        }
    }
}