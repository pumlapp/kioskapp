package com.zippy.zippykiosk;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.analytics.HitBuilders;
import com.zippy.zippykiosk.rest.CheckinResult;
import com.zippy.zippykiosk.rest.RetrofitUtils;
import com.zippy.zippykiosk.rest.ZippyApi;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by KB on 13/12/2015.
 * Copyright 2015 Zippy.com.au.
 */
public class CheckinAmountDialog  {
    private final MaterialDialog checkinDialog;
    private final String businessUri;
    private final String qrCode;
    private final String passcode;
    private TimeoutCallback timerCallback;
    private CheckinCallback checkinCallback;
    private final Handler mHandler = new Handler();

    static CheckinAmountDialog create(@NonNull Context context, @NonNull String businessUri, @NonNull String qrCode, @Nullable String passcode) {
        return new CheckinAmountDialog(context, businessUri, qrCode, passcode);
    }

    private final Runnable mTimeout = new Runnable() {
        @Override
        public void run() {
            dismiss();
        }
    };
    private CheckinAmountDialog(@NonNull Context context, @NonNull String businessUri, @NonNull String qrCode, @Nullable String passcode) {

        this.businessUri = businessUri;
        this.qrCode = qrCode;
        this.passcode = passcode;
        this.checkinDialog = new MaterialDialog.Builder(context)
                .title("How much did you spend?")
                .customView(R.layout.checkin_amount_layout, false)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .autoDismiss(false)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mHandler.removeCallbacksAndMessages(null);
                        onTimerStart();
                        onCheckinDismiss();
                    }
                })
                .onAny(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final @NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (which != DialogAction.POSITIVE || dialog.getActionButton(DialogAction.NEGATIVE).getVisibility()==View.GONE) {
                            dismiss();
                            return;
                        }
                        final View layout = dialog.getCustomView();
                        assert (layout != null);
                        final EditText amountEditText = (EditText) layout.findViewById(R.id.amount);
                        final EditText passcodeEditText = (EditText) layout.findViewById(R.id.passcode);
                        final TextView errorTextView = (TextView) layout.findViewById(R.id.error);
                        final View progress = layout.findViewById(R.id.progressBar);

                        errorTextView.setVisibility(View.GONE);

                        // Do some validation
                        String amount = amountEditText.getText().toString().trim();
                        float amountFloat = 0;
                        try {
                            amount = amount.replace("$", "");
                            amountFloat = Float.parseFloat(amount);
                            if (amountFloat <= 0) {
                                amountEditText.setError("Invalid amount");
                                amountEditText.requestFocus();
                                return;
                            }
                        } catch (Exception e) {
                            amountEditText.setError("Invalid amount");
                            amountEditText.requestFocus();
                            return;
                        }


                        if (!TextUtils.isEmpty(CheckinAmountDialog.this.passcode)) {
                            String pw = passcodeEditText.getText().toString();
                            if(!pw.equals(CheckinAmountDialog.this.passcode)) {
                                passcodeEditText.setError("Incorrect passcode");
                                passcodeEditText.requestFocus();
                                return;
                            }
                        }


                        final int centsSpent = (int)(amountFloat *100);
                        amountEditText.setEnabled(false);
                        passcodeEditText.setEnabled(false);
                        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                        dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
                        progress.setVisibility(View.VISIBLE);
                        onTimerStop();
                        dialog.setCancelable(false);
                        ZippyApi.getInstance().checkinAmount(CheckinAmountDialog.this.businessUri, CheckinAmountDialog.this.qrCode, amount, new Callback<CheckinResult>() {
                            @Override
                            public void success(CheckinResult checkinResult, Response response) {
                                dialog.setTitle(String.format("You earned %d point%s", checkinResult.pointsEarned, checkinResult.pointsEarned == 1 ? "" : "s"));
                                amountEditText.setVisibility(View.GONE);
                                passcodeEditText.setVisibility(View.GONE);
                                progress.setVisibility(View.GONE);
                                dialog.getActionButton(DialogAction.NEGATIVE).setVisibility(View.GONE);
                                dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                dialog.setCancelable(true);
                                onTimerStart();
                                onCheckinResult(checkinResult);
                                mHandler.postDelayed(mTimeout, 5000); // Close dialog after 5 secs

                                KioskApp.tracker.send(new HitBuilders.EventBuilder()
                                        .setCategory("Checkin Amount")
                                        .setValue(centsSpent)
                                        .build());
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                amountEditText.setEnabled(true);
                                passcodeEditText.setEnabled(true);
                                progress.setVisibility(View.GONE);
                                dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                                dialog.setCancelable(true);

                                onTimerStart();
                                String errorMsg = RetrofitUtils.getErrorDescription("Checkin Amount Failure.", error);
                                errorTextView.setText(errorMsg);
                                errorTextView.setVisibility(View.VISIBLE);

                                KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                                        .setDescription(errorMsg)
                                        .setFatal(false)
                                        .build());
                                CrashlyticsCore.getInstance().logException(new Exception("Checkin Amount Failure.", error));

                            }
                        });
                    }
                }).build();
        View layout = this.checkinDialog.getCustomView();
        assert(layout!=null);
        EditText amountEditText = (EditText)layout.findViewById(R.id.amount);
        amountEditText.addTextChangedListener(textWatcher);

        EditText passcodeEditText = (EditText)layout.findViewById(R.id.passcode);
        if(TextUtils.isEmpty(this.passcode)) {
            passcodeEditText.setVisibility(View.GONE);
            amountEditText.setImeActionLabel("OK", KeyEvent.KEYCODE_ENTER);
            amountEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handled = false;
                    if (actionId == KeyEvent.KEYCODE_ENTER) {
                        checkinDialog.getActionButton(DialogAction.POSITIVE).performClick();
                        handled = true;
                    }
                    return handled;
                }
            });
        }else {
            passcodeEditText.addTextChangedListener(textWatcher);
            passcodeEditText.setImeActionLabel("OK", KeyEvent.KEYCODE_ENTER);
            passcodeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handled = false;
                    if (actionId == KeyEvent.KEYCODE_ENTER) {
                        checkinDialog.getActionButton(DialogAction.POSITIVE).performClick();
                        handled = true;
                    }
                    return handled;
                }
            });
        }
    }


    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onTimerStart();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    public interface TimeoutCallback {
        void onTimerStart(@NonNull CheckinAmountDialog dialog);

        void onTimerStop(@NonNull CheckinAmountDialog dialog);
    }

    public interface CheckinCallback {
        void success(@NonNull CheckinAmountDialog dialog, @NonNull CheckinResult result);

        void dismiss(@NonNull CheckinAmountDialog dialog);
    }
    private void onCheckinResult(CheckinResult checkinResult) {
        if (this.checkinCallback != null) {
            this.checkinCallback.success(this,checkinResult);
        }
    }
    private void onCheckinDismiss() {
        if (this.checkinCallback != null) {
            this.checkinCallback.dismiss(this);
        }
    }
    private void onTimerStart() {
        if (this.timerCallback != null) {
            this.timerCallback.onTimerStart(this);
        }
    }
    private void onTimerStop() {
        if (this.timerCallback != null) {
            this.timerCallback.onTimerStop(this);
        }
    }

    public CheckinAmountDialog timeoutCallback(@NonNull CheckinAmountDialog.TimeoutCallback callback) {
        this.timerCallback = callback;
        return this;
    }
    public CheckinAmountDialog checkinCallback(@NonNull CheckinAmountDialog.CheckinCallback callback) {
        this.checkinCallback = callback;
        return this;
    }

    public void show() {

        this.checkinDialog.show();

    }
    private void dismiss() {
        this.checkinDialog.dismiss();
    }
}

