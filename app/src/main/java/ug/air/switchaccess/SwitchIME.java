package ug.air.switchaccess;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;

public class SwitchIME extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    String TAG = "SwitchIME";

    private InputMethodManager mInputMethodManager;

    private SwitchKeyboardView mInputView;
    private String[] mCompletions;

    private boolean mPredictionOn;
    private int mLastDisplayWidth;
    private LanguageModel mLanguageModel;
    private int mNumPredictions = 4;
    private boolean mLastThingEnteredWasSuggestedCompletion = false;
    private boolean mShiftPressedFlag = false;
    private int[] mSelectItemSwitchCodes;
    private int[] mNextItemSwitchCodes;
    private int mImeOptions;
    boolean mIsProcessingCandidatesFlag = false;
    boolean mAutoSpaceAndCapitalise = true;

    private SwitchKeyboard mSymbolsKeyboard;
    private SwitchKeyboard mAlphabeticKeyboard;
    private SwitchKeyboard mCurKeyboard;

    private String mWordSeparators;

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        new ParseLanguageModelTask().execute("languagemodel_en_GB.json");
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
        mCompletions = new String[mNumPredictions];
        for (int i = 0; i < mCompletions.length; i++) {
            mCompletions[i] = " ";
        }
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mAlphabeticKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }

        // QWERTY layout is the default option, but check when starting new input what the current
        // configuration setting is for the keyboard layou.
        mAlphabeticKeyboard = new SwitchKeyboard(this, R.xml.keyboard_qwerty);

        mSymbolsKeyboard = new SwitchKeyboard(this, R.xml.keyboard_symbols);
        //mSymbolsShiftedKeyboard = new SwitchKeyboard(this, R.xml.symbols_shift);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (SwitchKeyboardView) getLayoutInflater().inflate(
                R.layout.keyboard, null);
        mInputView.setOnKeyboardActionListener(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (prefs.getString("keyboard_layout", "qwerty").equals("optimised")) {
            mAlphabeticKeyboard = new SwitchKeyboard(this, R.xml.keyboard_optimised);
        }
        else {
            mAlphabeticKeyboard = new SwitchKeyboard(this, R.xml.keyboard_qwerty);
        }

        setLatinKeyboard(mAlphabeticKeyboard);

        mInputView.updatePredictionKeys(mCompletions);

        mSelectItemSwitchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_SELECTITEM);
        mNextItemSwitchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_NEXTITEM);

        return mInputView;
    }


    private void setLatinKeyboard(SwitchKeyboard nextKeyboard) {
        mInputView.setKeyboard(nextKeyboard);
    }


    /**
     * Read in the language model and populate the initial predictions.
     */
    private class ParseLanguageModelTask extends AsyncTask<String, Integer, LanguageModel> {
        protected LanguageModel doInBackground(String... langModelFile) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            String json = null;
            try {
                InputStream inputStream = getAssets().open(langModelFile[0]);
                int size = inputStream.available();
                byte[] buffer = new byte[size];
                inputStream.read(buffer);
                inputStream.close();
                json = new String(buffer, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return gson.fromJson(json, LanguageModel.class);
        }

        protected void onPostExecute(LanguageModel languageModel) {
            mLanguageModel = languageModel;
            updateShiftKeyState();
            updateCandidates();
        }
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);


        // Find out which type of alphabetic keyboard the user has selected
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (prefs.getString("keyboard_layout", "qwerty").equals("optimised")) {
            mAlphabeticKeyboard = new SwitchKeyboard(this, R.xml.keyboard_optimised);
        }
        else {
            mAlphabeticKeyboard = new SwitchKeyboard(this, R.xml.keyboard_qwerty);
        }

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        //mPreviousWord = "";
        mPredictionOn = true;
        mAutoSpaceAndCapitalise = true;

        for (int i=0;i<mNumPredictions;i++) {
            mCompletions[i] = " ";
        }

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
                mCurKeyboard = mSymbolsKeyboard;
                mPredictionOn = false;
                break;

            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                mPredictionOn = false;
                break;

            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                mPredictionOn = false;
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mAlphabeticKeyboard;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                    mAutoSpaceAndCapitalise = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
                    mPredictionOn = true;
                    mAutoSpaceAndCapitalise = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
                    // Our predictions are not useful for e-mail addresses
                    mPredictionOn = false;
                    mAutoSpaceAndCapitalise = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // It's possible to disable predictions if the field is marked as
                    // auto-completing.
                    mPredictionOn = true;
                }

                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mAlphabeticKeyboard;
                //updateShiftKeyState(attribute);
        }


        mImeOptions = attribute.imeOptions;
        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);

        // don't want to enter a URL for example and find it's all caps
        if (!mAutoSpaceAndCapitalise && mInputView!=null) {
            mInputView.setShifted(false);
        }
    }

    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.

        setLatinKeyboard(mAlphabeticKeyboard);

        mSelectItemSwitchCodes = SwitchboardPreferences.getSwitchCodes(getBaseContext(),SwitchboardPreferences.SWITCH_TYPE_SELECTITEM);
        setLatinKeyboard(mCurKeyboard);
        mInputView.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
        updateShiftKeyState(attribute);
        updateCandidates();
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();

        mCurKeyboard = mAlphabeticKeyboard;
        if (mInputView != null) {
            clearCandidates();
            mInputView.closing();
        }
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                            int newSelStart, int newSelEnd,
                                            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        updateShiftKeyState();
        updateCandidates();
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (!isInputViewShown()) {
            return false;
        }

        //int keyCodeToProcess = keyCode;
        int[] keyCodes = {0,0,0}; // The zeros distinguish from double-click key events from IME itself
        boolean selectItemSwitchPressed = false;
        boolean nextItemSwitchPressed = false;

        // Was the selection switch pressed?
        if (mSelectItemSwitchCodes!=null) {
            for (int sc : mSelectItemSwitchCodes) {
                if (keyCode==sc) {
                    selectItemSwitchPressed = true;
                }
            }
        }

        // Or was the "next item" switch pressed?
        if (mNextItemSwitchCodes!=null) {
            for (int sc : mNextItemSwitchCodes) {
                if (keyCode==sc) {
                    nextItemSwitchPressed = true;
                }
            }
        }

        // Swallow any auto-repeats from the switch
        if ((selectItemSwitchPressed || nextItemSwitchPressed) && event.getRepeatCount()>0) {
            return true;
        }

        if (selectItemSwitchPressed) {
            int softKeyFiredCode = mInputView.activateSelection();
            if (softKeyFiredCode!=0) {
                onKey(softKeyFiredCode,keyCodes);
            }
            return true;
        }
        else if (nextItemSwitchPressed) {
            mInputView.advanceSelection();
            return true;
        }
        else {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    // The InputMethodService already takes care of the back
                    // key for us, to dismiss the input method if it is shown.
                    // However, our keyboard could be showing a pop-up window
                    // that back should dismiss, so we first allow it to do that.
                    if (event.getRepeatCount() == 0 && mInputView != null) {
                        if (mInputView.handleBack()) {
                            return true;
                        }
                    }
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    // Let the underlying text editor always handle these.
                    return false;
            }
        }
        return false;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean
    onKeyUp(int keyCode, KeyEvent event) {

        if (!isInputViewShown()) {
            return false;
        }

        boolean switchPressed = false;
        if (mSelectItemSwitchCodes!=null) {
            for (int sc : mSelectItemSwitchCodes) {
                if (keyCode==sc) {
                    switchPressed = true;
                }
            }
        }

        if (mNextItemSwitchCodes!=null) {
            for (int sc : mNextItemSwitchCodes) {
                if (keyCode==sc) {
                    switchPressed = true;
                }
            }
        }

        // Swallow any key down switch events
        if (switchPressed) {
            return true;
        }

        return false;
    }

    public void onKey(int primaryCode, int[] keyCodes) {

        if ((primaryCode==-5 && keyCodes.length==1) || (primaryCode>0 && primaryCode<6 && keyCodes.length>0 && keyCodes[0] != primaryCode)) {
            // Ignore keycodes which come from double/triple taps
            // These are just key positions stored in the xml codes array
            return;
        }

        if (primaryCode<-1000) {  // Suggestion key has been pressed
            int predictionIndex = -1001 - primaryCode;
            if (mCompletions!=null && mCompletions.length>=(predictionIndex+1) && mCompletions[predictionIndex].length()>0 && !mCompletions[predictionIndex].equals(" ")) {
                // A predicted word may already have been partially typed - only add characters
                // which have not already been committed.
                String completionText = mCompletions[predictionIndex];
                String prevText = getCurrentInputConnection().getTextBeforeCursor(completionText.length(), 0).toString();
                // Count backwards until reaching a space or separator
                int nCharsToSkip     = 0;
                for (int i=prevText.length()-1;i>=0;i--) {
                    if (mWordSeparators.contains(String.valueOf(prevText.charAt(i)))) {
                        break;
                    }
                    nCharsToSkip++;
                }
                completionText = completionText.substring(nCharsToSkip);

                getCurrentInputConnection().commitText(completionText + " ", 1);
                mInputView.setShifted(false);
                updateCandidates();
                mLastThingEnteredWasSuggestedCompletion = true;
            }
        }
        else {
            if (isWordSeparator(primaryCode)) {
                // If there's a space beforehand from accepting a prediction, delete it.
                if (mLastThingEnteredWasSuggestedCompletion) {
                    CharSequence prevText = getCurrentInputConnection().getTextBeforeCursor(1, 0);
                    if (prevText.length() > 0 && prevText.charAt(0) == ' ') {
                        getCurrentInputConnection().deleteSurroundingText(1, 0);
                    }
                }

                String textToCommit = String.valueOf((char) primaryCode);

                // After some characters add a space.
                if (mAutoSpaceAndCapitalise) {
                    if ((char) primaryCode=='.' || (char) primaryCode==',' || (char) primaryCode==':'
                            || (char) primaryCode==';' || (char) primaryCode==')' || (char) primaryCode=='!'
                            || (char) primaryCode=='?')
                    {
                        textToCommit += " ";
                    }
                }

                // Enter is handled separately, but everything else gets committed
                if ((char) primaryCode!='\n') {
                    getCurrentInputConnection().commitText(textToCommit,1);
                }
            } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
                handleBackspace();
            } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
                mShiftPressedFlag = true;
                handleShift();
            } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
                handleClose();
                return;
            } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                    && mInputView != null) {
                Keyboard current = mInputView.getKeyboard();
                if (current == mSymbolsKeyboard) {
                    setLatinKeyboard(mAlphabeticKeyboard);
                    mPredictionOn = true;
                    updateCandidates();
                } else {
                    clearCandidates();
                    mPredictionOn = false;
                    setLatinKeyboard(mSymbolsKeyboard);
                    mSymbolsKeyboard.setShifted(false);
                }
            }
            else {
                sendKey(primaryCode);
            }

            // Special handling of enter key, depending on editor options
            if ((char) primaryCode=='\n') {
                switch (mImeOptions&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
                    case EditorInfo.IME_ACTION_GO:
                        getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_GO);
                        break;
                    case EditorInfo.IME_ACTION_NEXT:
                        getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_NEXT);
                        break;
                    case EditorInfo.IME_ACTION_SEARCH:
                        getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_SEARCH);
                        break;
                    case EditorInfo.IME_ACTION_SEND:
                        getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_SEND);
                        break;
                    default:
                        sendKey(primaryCode);
                }
            }

            mLastThingEnteredWasSuggestedCompletion = false;
            if (primaryCode != Keyboard.KEYCODE_SHIFT) {
                mShiftPressedFlag = false;
            }
        }
        // Don't need to call these because they are called in onUpdateSelection callback from editor
        updateShiftKeyState();
        updateCandidates();
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateCandidates();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    if (mInputView.isShifted()) {
                        keyCode = Character.toUpperCase(keyCode);
                    }
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    private void handleBackspace() {
        keyDownUp(KeyEvent.KEYCODE_DEL);
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mAlphabeticKeyboard == currentKeyboard) {
            mInputView.setShifted(!mInputView.isShifted());
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        // for this app, we don't actually care about what kind of keyboard it is as shift only
        // affects the alphabetic keyboards.
        updateShiftKeyState();
    }

    /* Update shift key based on preceding text */
    private void updateShiftKeyState() {

        if (mInputView!=null && !mShiftPressedFlag && mAutoSpaceAndCapitalise) {
            // Find out if we're supposed to use caps in this type of input
            boolean beginningOfSentence = false;

            if (getCurrentInputConnection() != null) {
                CharSequence prevText = getCurrentInputConnection().getTextBeforeCursor(10,0);

                // If there's no preceding text, then it's the beginning of a sentence.
                if (prevText==null) {
                    beginningOfSentence = true;
                }
                else if (prevText.length()==0) {
                    beginningOfSentence = true;
                }
                else {
                    // Also check if the most recent non-space character is a sentence separator.
                    for (int i=prevText.length()-1;i>=0;i--) {
                        if (prevText.charAt(i)!=' ') {
                            if (prevText.charAt(i)=='.' || prevText.charAt(i)=='!' ||
                                    prevText.charAt(i)=='?' || prevText.charAt(i)=='\n') {
                                beginningOfSentence = true;
                                break;
                            }
                            else {
                                beginningOfSentence = false;
                                break;
                            }
                        }
                    }
                }
            }
            mInputView.setShifted(beginningOfSentence);
        }
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (mInputView!=null) {
            String composing = "";
            String previousWord = "";
            String[] words;

            if (mPredictionOn && getCurrentInputConnection() != null && mLanguageModel != null) {
                try {
                    String prevText = getCurrentInputConnection().getTextBeforeCursor(50, 0).toString();
                    if (prevText != null && prevText.length() > 0) {
                        words = prevText.split(" |\n");
                        if (prevText.charAt(prevText.length() - 1) == ' ' || prevText.charAt(prevText.length() - 1) == '\n') {
                            if (words.length > 0) {
                                previousWord = words[words.length - 1];
                            }
                        } else {
                            composing = words[words.length - 1];
                            if (words.length > 1) {
                                previousWord = words[words.length - 2];
                            }
                        }
                        // Check if a new sentence has been started
                        if (previousWord.contains(".") ||
                                previousWord.contains("!") ||
                                previousWord.contains("?") ||
                                previousWord.contains("\n")) {
                            previousWord = "";
                        }
                    }

                    if (!mIsProcessingCandidatesFlag) {
                        new UpdateCandidatesAsync().execute(previousWord,composing);
                    }
                }
                catch (Exception e) {
                    // keyboard wasn't ready yet
                    Log.e(TAG,"error in updateCandidates: " + Log.getStackTraceString(e));
                }
            }
        }
        else {
            for (int i = 0; i < mCompletions.length; i++) {
                mCompletions[i] = " ";
            }
        }
    }

    /**
     * Read in the language model and populate the initial predictions.
     */
    private class UpdateCandidatesAsync extends AsyncTask<String, Integer, String[]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsProcessingCandidatesFlag = true;
        }

        protected String[] doInBackground(String... editContext) {
            String[] candidates = new String[mNumPredictions];
            for (int i=0;i<mNumPredictions;i++) {
                candidates[i] = " ";
            }

            if (editContext.length<2) {
                return candidates;
            }

            String previousWord = editContext[0];
            String composing = editContext[1];

            candidates = mLanguageModel.mostFrequentWords(previousWord.toLowerCase(), composing.toLowerCase(), mNumPredictions);

            // Capitalise if we're at the beginning of a sentence
            if ((composing.length() > 0 && Character.isUpperCase(composing.charAt(0)))) {
                for (int i = 0; i < candidates.length; i++) {
                    candidates[i] = candidates[i].substring(0, 1).toUpperCase() + candidates[i].substring(1);
                }
            }
            return candidates;
        }

        protected void onPostExecute(String[] candidates) {
            // Capitalise if currently shifted, which we can only check from the UI thread
            if (mInputView.isShifted()) {
                for (int i = 0; i < candidates.length; i++) {
                    candidates[i] = candidates[i].substring(0, 1).toUpperCase() + candidates[i].substring(1);
                }
            }
            mCompletions = candidates;
            mInputView.updatePredictionKeys(candidates);

            // TODO: Check if there's more to process, and if so launch a new task

            mIsProcessingCandidatesFlag = false;
        }
    }


    private void clearCandidates() {
        for (int i=0;i<mCompletions.length;i++) {
            mCompletions[i] = " "; // empty strings cause crash in adjustCase
        }
        if (mInputView!=null) {
            mInputView.updatePredictionKeys(mCompletions);
        }
    }

    private void handleClose() {

        requestHideSelf(0);
        mInputView.closing();
    }

    public boolean isWordSeparator(int code) {
        return mWordSeparators.contains(String.valueOf((char)code));
    }

    public void swipeRight() {}

    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {}

    public void onPress(int primaryCode) {}

    public void onRelease(int primaryCode) {}
}
