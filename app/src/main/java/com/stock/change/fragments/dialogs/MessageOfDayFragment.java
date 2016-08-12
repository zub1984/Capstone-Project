package com.stock.change.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.stock.change.R;
import com.stock.change.custom.MyApplication;


public class MessageOfDayFragment extends DialogFragment{
    public static final String TAG = MessageOfDayFragment.class.getSimpleName();

    private MaterialDialog mDialog;
    private TextView mText;
    private View mProgressWheel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.navigation_motd)
                .customView(R.layout.dialog_custom_motd, true)
                .positiveText(R.string.dialog_close)
                .build();

        View customView = mDialog.getCustomView();
        if(customView != null) {
            mText = (TextView) customView.findViewById(R.id.text_motd);
            mProgressWheel = customView.findViewById(R.id.progress_wheel);
        }

        updateMsgOfTheDay();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return mDialog;
    }

    private void updateMsgOfTheDay(){
        ContainerHolder containerHolder = MyApplication.getInstance().getContainerHolder();
        if(containerHolder != null) {
            //http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable
            Spanned text = Html.fromHtml(containerHolder.getContainer()
                    .getString(getString(R.string.tag_manager_motd_key)));
            URLSpan[] currentSpans = text.getSpans(0, text.length(), URLSpan.class);
            SpannableString buffer = new SpannableString(text);
            Linkify.addLinks(buffer, Linkify.ALL);

            for (URLSpan span : currentSpans) {
                int end = text.getSpanEnd(span);
                int start = text.getSpanStart(span);
                buffer.setSpan(span, start, end, 0);
            }
            mText.setText(buffer);
            mText.setMovementMethod(LinkMovementMethod.getInstance());

        }else{
            mText.setText(getString(R.string.dialog_motd));
        }

        mProgressWheel.setVisibility(View.INVISIBLE);
    }
}
