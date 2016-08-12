package com.stock.change.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stock.change.R;


public class FaqDialog extends DialogFragment {
    public static final String TAG = FaqDialog.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.navigation_faq)
                .content(R.string.dialog_faq)
                .positiveText(R.string.dialog_close)
                .build();
    }
}
