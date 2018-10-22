package com.zippy.zippykiosk.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;

import com.zippy.zippykiosk.R;

/**
 * Created by kimb on 24/05/2015.
 */
public class ClearableAutoCompleteEditText extends AutoCompleteTextView {

    public String defaultValue = "";
    final Drawable imgX = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_clear_search_api_holo_light, null); // X image


    public ClearableAutoCompleteEditText(Context context) {
        super(context);

        init();
    }

    public ClearableAutoCompleteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    public ClearableAutoCompleteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    void init() {

        // Set bounds of our X button
        imgX.setBounds(0, 0, imgX.getIntrinsicWidth(), imgX.getIntrinsicHeight());

        // There may be initial text in the field, so we may need to display the button
        //manageClearButton();

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                ClearableAutoCompleteEditText et = ClearableAutoCompleteEditText.this;

                // Is there an X showing?
                if (et.getCompoundDrawables()[2] == null) return false;
                // Only do this for up touches
                if (event.getAction() != MotionEvent.ACTION_UP) return false;
                // Is touch on our clear button?
                if (event.getX() > et.getWidth() - et.getPaddingRight() - imgX.getIntrinsicWidth()) {
                    et.setText("");
                    ClearableAutoCompleteEditText.this.removeClearButton();
                }
                return false;
            }
        });

        this.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ClearableAutoCompleteEditText.this.isFocused()) {
                    ClearableAutoCompleteEditText.this.manageClearButton();
                }
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        this.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    manageClearButton();
                } else
                    removeClearButton();
            }
        });
    }

    void manageClearButton() {
        if (this.getText().toString().equals(""))
            removeClearButton();
        else
            addClearButton();
    }

    void addClearButton() {
        this.setCompoundDrawables(this.getCompoundDrawables()[0],
                this.getCompoundDrawables()[1],
                imgX,
                this.getCompoundDrawables()[3]);
    }

    void removeClearButton() {
        this.setCompoundDrawables(this.getCompoundDrawables()[0],
                this.getCompoundDrawables()[1],
                null,
                this.getCompoundDrawables()[3]);
    }

}
