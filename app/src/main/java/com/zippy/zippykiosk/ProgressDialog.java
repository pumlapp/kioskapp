package com.zippy.zippykiosk;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Created by kimb on 11/07/2015.
 */
public class ProgressDialog extends DialogFragment {
    private String mContent;

    public static ProgressDialog create(String content) {
        ProgressDialog dialog = new ProgressDialog();
        Bundle args = new Bundle();
        args.putString("content", content);
        dialog.setArguments(args);
        return dialog;
    }

    public ProgressDialog() {
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title("") // Set title so we can hide/show if error
                .customView(R.layout.progress_layout, false)
                .cancelable(false)
                .build();

        dialog.getTitleView().setVisibility(View.GONE);
        View v = dialog.getCustomView();
        assert v != null;
        if(mContent==null) {
            mContent = getArguments().getString("content", "");
        }
        ((TextView)v.findViewById(R.id.content)).setText(mContent);

        // Set custom size of dialog
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        int minPixelWidth = Utils.convertDIPToPixels(getActivity(), 600);
        if (minPixelWidth < size.x) {
            lp.width = minPixelWidth;
            dialog.getWindow().setAttributes(lp);
        }

        //Here's the magic.. Set the dialog to not focusable (makes navigation ignore us adding the window)
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        dialog.getWindow().getDecorView().setSystemUiVisibility(getActivity().getWindow().getDecorView().getSystemUiVisibility());
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.setCancelable(false);
        return dialog;
    }

    public void show(Activity activity) {
        show(activity.getFragmentManager(), "PROGRESS_DIALOG");
    }



    public void setContent(String content) {
        MaterialDialog dialog = (MaterialDialog) getDialog();
        if(dialog!=null) {
            View v = dialog.getCustomView();
            assert v != null;
            ((TextView) v.findViewById(R.id.content)).setText(content);
        }else {
            mContent = content;
        }
    }
    public void showError(String title, CharSequence message) {
        MaterialDialog dialog = (MaterialDialog) getDialog();
        if(dialog!=null) {
            dialog.setCancelable(true);

            View customView = dialog.getCustomView();
            assert customView != null;
            ((TextView) customView.findViewById(R.id.content)).setText(message);
            customView.findViewById(R.id.progressBar).setVisibility(View.GONE);

            dialog.setTitle(title);
            dialog.getTitleView().setVisibility(View.VISIBLE);
            dialog.setActionButton(DialogAction.POSITIVE, android.R.string.ok);

        }
    }
    public void showSuccess(String title, CharSequence message) {
        MaterialDialog dialog = (MaterialDialog) getDialog();
        if(dialog!=null) {
            dialog.setCancelable(true);

            View customView = dialog.getCustomView();
            assert customView != null;
            ((TextView) customView.findViewById(R.id.content)).setText(message);
            customView.findViewById(R.id.progressBar).setVisibility(View.GONE);

            dialog.setTitle(title);
            dialog.getTitleView().setVisibility(View.VISIBLE);
            dialog.setActionButton(DialogAction.POSITIVE, android.R.string.ok);

        }
    }

}
