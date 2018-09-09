package ug.air.switchaccess;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodSubtype;

import java.util.List;
import java.lang.Runnable;

import static android.content.Context.MODE_PRIVATE;

public class SwitchKeyboardView extends KeyboardView {

    private int[] mCurrentSelected = {1,0,0};
    private int mCurrentSelectionLevel = 1;
    boolean mLastKeyWasDelete = false;

    private Handler mHandler;
    private Paint mPaint;
    private Rect mPadding;
    SharedPreferences mPrefs;

    int mStepTime = 1000;

    String mKeyTextColour = "#d4d6d7";
    String mCircleIconBackgroundColour = "#a7a7a7";
    int mCircleIconRadius = 17;
    int mLabelTextSize = 17;
    int mKeyTextSize = 22;
    boolean autoScan = true;

    public Boolean predictionsToShow = false;

    String TAG = "SwitchKeyboardView";

    void advanceSelection() {
        if (mLastKeyWasDelete) {
            // hang around on this key, as it's likely to be needed again
            mLastKeyWasDelete = false;
        }
        else {
            int max_val_at_level = 0;
            switch (mCurrentSelectionLevel) {
                case 1:
                    if (predictionsToShow) {
                        max_val_at_level = 3;
                    } else {
                        max_val_at_level = 2;
                    }
                    break;
                case 2:
                    max_val_at_level = 4;
                    break;
                case 3:
                    max_val_at_level = 5;
            }

            if (mCurrentSelected[mCurrentSelectionLevel - 1] < max_val_at_level) {
                mCurrentSelected[mCurrentSelectionLevel - 1] += 1;
            } else {
                mCurrentSelectionLevel = 1;
                mCurrentSelected[0] = 1;
                mCurrentSelected[1] = 0;
                mCurrentSelected[2] = 0;
            }
        }
        updateKeyboardScanSpeed();
        invalidateAllKeys();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            // TODO: dynamically work out how many keys there are from the xml layout
            try {
                advanceSelection();
            } finally {
                mHandler.postDelayed(mStatusChecker, mStepTime);
            }
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    Context context;
    public SwitchKeyboardView(Context context, AttributeSet attrs) {

        super(context, attrs);
        this.context = context ;
        mPrefs = context.getSharedPreferences(SwitchboardPreferences.GENERAL_SETTINGS_FILE_KEY, MODE_PRIVATE);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAlpha(255);
        mPadding = new Rect(0, 0, 0, 0);
        mHandler = new Handler();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        autoScan = prefs.getString("keyboard_scan_type", "auto").equals("auto");
        if (autoScan) {
            startRepeatingTask();
        }
        setPreviewEnabled(false);
    }

    private CharSequence adjustCase(CharSequence label) {
        if (this.isShifted() && label != null && label.length() == 1
                && Character.isLowerCase(label.charAt(0))) {
            label = label.toString().toUpperCase();
        }
        return label;
    }

    void updateKeyboardScanSpeed() {
        double x = (double) 100-mPrefs.getInt("keyboardScanSpeed",40);
        mStepTime = (int) (300. + .46*Math.pow(x,2) - 9.*x);
        // Solution to quadratic curve fitting:
        // 100 -> 300ms
        // 50 -> 1000ms
        // 0 -> 5000ms
    }

    public int activateSelection() {
        int keyFiredCode = 0;
        boolean keySelected = false;

        if (mCurrentSelectionLevel == 1) {
            mCurrentSelectionLevel = 2;
            mCurrentSelected[1] = 0;
        } else if (mCurrentSelectionLevel == 2) {
            if (mCurrentSelected[0] == 3) {
                keySelected = true;
            } else {
                mCurrentSelectionLevel = 3;
                mCurrentSelected[2] = 0;
            }
        } else {
            keySelected = true;
        }

        stopRepeatingTask();
        if (keySelected) {
            List<Keyboard.Key> keys = getKeyboard().getKeys();
            for (Keyboard.Key key : keys) {
                if (key.codes[1] == mCurrentSelected[0] && key.codes[2] == mCurrentSelected[1] && key.codes[3] == mCurrentSelected[2]) {
                    keyFiredCode = key.codes[0];
                    break;
                }

            }

            if (keyFiredCode == Keyboard.KEYCODE_DELETE) {
                //Log.e(TAG, "Delete key");
                mLastKeyWasDelete = true;
            } else {
                mCurrentSelectionLevel = 1;
                mCurrentSelected[0] = 0;
                mCurrentSelected[1] = 0;
                mCurrentSelected[2] = 0;

            }
        }
        if (autoScan) {
            startRepeatingTask();
        }
        else {
            advanceSelection();
        }

        return keyFiredCode;
    }

    public void updatePredictionKeys(String[] suggestions) {
        List<Keyboard.Key> keys = getKeyboard().getKeys();
        for (Keyboard.Key key : keys) {
            if (key.codes[0]<-1000) {
                int predictionIndex = -1001 - key.codes[0];
                key.label = suggestions[predictionIndex];

            }
        }
        predictionsToShow = suggestions[0].length()>0 && suggestions[0].charAt(0) != ' ' ;
        invalidateAllKeys();
    }

    @Override
    public void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        List<Keyboard.Key> keys = getKeyboard().getKeys();
        float scaledSizeInPixels;
        float radius;
        Paint paint = mPaint;
        Rect padding = mPadding;

        Drawable dr = (Drawable) ResourcesCompat.getDrawable(getResources(), R.drawable.normal, null);
        dr.setBounds(0,0, canvas.getWidth(), canvas.getHeight());
        dr.draw(canvas);

        for (Keyboard.Key key : keys) {
            if ((mCurrentSelectionLevel==1 && key.codes[1]==mCurrentSelected[0])
                    || (mCurrentSelectionLevel==2 && key.codes[1]==mCurrentSelected[0] && key.codes[2]==mCurrentSelected[1])
                    || (mCurrentSelectionLevel==3 && key.codes[1]==mCurrentSelected[0] && key.codes[2]==mCurrentSelected[1] && key.codes[3]==mCurrentSelected[2]))
            {
                if (key.codes[0]<-1000) {
                    dr = (Drawable) ResourcesCompat.getDrawable(getResources(), R.drawable.highlighted_prediction, null);
                } else {
                    dr = (Drawable) ResourcesCompat.getDrawable(getResources(), R.drawable.highlighted, null);
                }
                dr.setBounds(key.x, key.y, key.x + key.width, key.y + key.height);
                dr.draw(canvas);
            } else if (key.codes[0]<-1000) {
                dr = (Drawable) ResourcesCompat.getDrawable(getResources(), R.drawable.prediction, null);
                dr.setBounds(key.x, key.y, key.x + key.width, key.y + key.height);
                dr.draw(canvas);
            }

            String label = key.label == null? null : adjustCase(key.label).toString();

            paint.setColor(Color.parseColor(mKeyTextColour));

            if (label != null) {
                // For characters, use large font. For labels like "Done", use small font.
                if (key.codes[0]<-1000 || key.codes[0]==-2) {
                    scaledSizeInPixels = mLabelTextSize * getResources().getDisplayMetrics().scaledDensity;
                    paint.setTextSize(scaledSizeInPixels);
                    paint.setTypeface(Typeface.DEFAULT);
                } else {
                    scaledSizeInPixels = mKeyTextSize * getResources().getDisplayMetrics().scaledDensity;
                    paint.setTextSize(scaledSizeInPixels);
                    paint.setTypeface(Typeface.DEFAULT);
                }
                // Draw the text
                TextPaint textPaint = new TextPaint(paint);
                CharSequence labelFitted = TextUtils.ellipsize(label,textPaint,key.width,TextUtils.TruncateAt.MARQUEE);
                canvas.drawText(labelFitted.toString(),
                        key.x + (key.width - padding.left - padding.right) / 2
                                + padding.left,
                        key.y + (key.height - padding.top - padding.bottom) / 2
                                + (paint.getTextSize() - paint.descent()) / 2 + padding.top,
                        paint);

            } else if (key.icon != null) {
                final int drawableX = key.x + (key.width - padding.left - padding.right
                        - key.icon.getIntrinsicWidth()) / 2 + padding.left;
                final int drawableY = key.y + (key.height - padding.top - padding.bottom
                        - key.icon.getIntrinsicHeight()) / 2 + padding.top;
                canvas.translate(drawableX, drawableY);

                if (key.codes[0]==10) {
                    radius = mCircleIconRadius * getResources().getDisplayMetrics().scaledDensity;;
                    paint.setColor(Color.parseColor(mCircleIconBackgroundColour));
                    canvas.drawCircle(key.icon.getIntrinsicWidth()/2,key.icon.getIntrinsicHeight()/2,radius,paint);
                }

                paint.setColor(Color.parseColor(mKeyTextColour));

                key.icon.setBounds(0, 0,
                        key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());
                key.icon.draw(canvas);
                canvas.translate(-drawableX, -drawableY);
            }
        }
    }

    public SwitchKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean onLongPress(Keyboard.Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            //getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
    }

    void setSubtypeOnSpaceKey(final InputMethodSubtype subtype) {
        final SwitchKeyboard keyboard = (SwitchKeyboard)getKeyboard();
        invalidateAllKeys();
    }
}
