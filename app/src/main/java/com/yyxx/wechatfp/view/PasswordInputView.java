package com.yyxx.wechatfp.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.yyxx.wechatfp.util.DpUtil;

/**
 * Created by Jason on 2017/9/9.
 */

public class PasswordInputView extends DialogFrameLayout {

    public static final String DEFAULT_HIDDEN_PASS = "123zxc456asdxxx";

    private EditText mInputView;

    public PasswordInputView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public PasswordInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PasswordInputView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        mInputView = new EditText(context);
        mInputView.setFocusable(true);
        mInputView.setFocusableInTouchMode(true);
        mInputView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        mInputView.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        LayoutParams layoutParam = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int defHMargin = DpUtil.dip2px(context, 15);
        int defTMargin = DpUtil.dip2px(context, 30);
        int defBMargin = DpUtil.dip2px(context, 4);
        layoutParam.setMargins(defHMargin, defTMargin, defHMargin, defBMargin);

        this.addView(mInputView, layoutParam);
    }

    @NonNull
    public String getInput() {
        Editable ediable = mInputView.getText();
        if (ediable == null) {
            return "";
        }
        return ediable.toString();
    }

    public void setDefaultText(String text) {
        mInputView.setText(text);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mInputView, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Override
    public String getDialogTitle() {
        return "請輸入密碼";
    }
}
