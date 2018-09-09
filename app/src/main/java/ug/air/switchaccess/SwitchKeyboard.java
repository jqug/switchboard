package ug.air.switchaccess;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.support.v4.content.res.ResourcesCompat;
import android.view.inputmethod.EditorInfo;

public class SwitchKeyboard extends Keyboard {

    private Key mEnterKey;

    public SwitchKeyboard(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
    }

    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y,
                                   XmlResourceParser parser) {
        Key key = new LatinKey(res, parent, x, y, parser);
        if (key.codes[0] == 10) {
            mEnterKey = key;
        }
        return key;
    }

    /**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    void setImeOptions(Resources res, int options) {
        if (mEnterKey == null) {
            return;
        }

        switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {

            case EditorInfo.IME_ACTION_GO:
                mEnterKey.iconPreview = null;
                //mEnterKey.icon = null;
                //mEnterKey.label = res.getText(R.string.label_go_key);
                //mEnterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.sym_keyboard_return, null);
                mEnterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.ic_arrow_forward_white_24dp, null);
                // = res.getDrawable(R.drawable.sym_keyboard_return);

                break;
            case EditorInfo.IME_ACTION_NEXT:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                //mEnterKey.label = res.getText(R.string.label_next_key);

                //mEnterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.sym_keyboard_return, null);
                mEnterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.ic_arrow_forward_white_24dp, null);
                // = res.getDrawable(R.drawable.sym_keyboard_return);

                break;
            case EditorInfo.IME_ACTION_SEARCH:
                mEnterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.ic_magnify_white_24dp, null);
                // = res.getDrawable(R.drawable.sym_keyboard_search);
                mEnterKey.label = null;
                break;
            case EditorInfo.IME_ACTION_SEND:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                //mEnterKey.label = res.getText(R.string.label_send_key);
                //mEnterKey.label = "\u23ce";
                //mEnterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.sym_keyboard_return, null);
                mEnterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.ic_keyboard_return_white_24dp, null);
                // = res.getDrawable(R.drawable.sym_keyboard_return);
                break;
            default:
                //mEnterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.sym_keyboard_return, null);
                mEnterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.ic_keyboard_return_white_24dp, null);
                // = res.getDrawable(R.drawable.sym_keyboard_return);
                //mEnterKey.label = "\u23ce";
                break;
        }
    }

    static class LatinKey extends Keyboard.Key {

        public LatinKey(Resources res, Keyboard.Row parent, int x, int y,
                        XmlResourceParser parser) {
            super(res, parent, x, y, parser);
        }
    }
}
