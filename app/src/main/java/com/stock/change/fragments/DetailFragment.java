package com.stock.change.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.stock.change.BarChartActivity;
import com.stock.change.R;
import com.stock.change.data.StockContract;
import com.stock.change.events.LoadDetailFinishedEvent;
import com.stock.change.services.DetailService;
import com.stock.change.utils.Constants;
import com.stock.change.utils.Utility;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Fragment that contains more details of the list items in the main list.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
    public static final String TAG = DetailFragment.class.getSimpleName();

    /**
     * define the butter knife binding variables
     */
    @BindView(R.id.detail_main_info) GridLayout mMainInfo;
    @BindView(R.id.detail_extras_info) GridLayout mExtrasInfo;
    @BindView(R.id.progress_wheel) ProgressBar mProgressWheel;
    @BindView(R.id.text_update_time) TextView mTextUpdateTime;
    @BindView(R.id.text_streak_prev) TextView mTextPrevStreak;
    @BindView(R.id.button_retry) Button mRetryButton;
    @BindView(R.id.button_bar_chart) ImageView mBarChart;
    @BindString(R.string.update_time_format) String mUpdateTimeFormat;
    @BindView(R.id.toolbar) Toolbar mToolBar;

   /* @BindString(R.string.placeholder_days)
    String mDays;
    @BindString(R.string.placeholder_day)
    String mDay;
    @BindString(R.string.placeholder_dollar)
    String mDollar;*/

    private Unbinder unbinder;

    private Uri mDetailUri;
    private boolean mReplyButtonVisible;
    private boolean mIsDetailRequestLoading;
    private Toast mBarChartButtonToast;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Need to postpone transition until data is loaded because if data is loading while
            // transition is happening setting visibility of some items to VISIBLE will make the
            // items appear instantly instead of being transitioned.
            getActivity().postponeEnterTransition();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG,"onCreateView");
        View view = inflater.inflate(R.layout.fragment_detail_ref, container, false);
        unbinder = ButterKnife.bind(this, view);
        Log.i(TAG,"ButterKnife.bind");
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.i(TAG,"onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        mDetailUri = args.getParcelable(Constants.KEY_DETAIL_URI);

        if (savedInstanceState != null) {
            mReplyButtonVisible = savedInstanceState.getBoolean(Constants.KEY_REPLY_BUTTON_VISIBLE);
            mIsDetailRequestLoading = savedInstanceState.getBoolean(Constants.KEY_IS_DETAIL_REQUEST_LOADING);
        }

        if (getResources().getBoolean(R.bool.is_phone)) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(mToolBar);
            ActionBar actionBar = activity.getSupportActionBar();

            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            setHasOptionsMenu(true);

        } else {
            mToolBar.setVisibility(View.GONE);
        }

        mRetryButton.setOnClickListener(this);
        mBarChart.setOnClickListener(this);

    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus eventBus = EventBus.getDefault();
        eventBus.register(this);

        if (mTextPrevStreak.getText().toString().isEmpty()) {
            fetchDetailsData();
        }
    }

    /**
     * Fetches the detail data from the db of the selected stock using a cursor loader.
     */
    private void fetchDetailsData() {
        showProgressWheel();
        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
        loaderManager.restartLoader(Constants.ID_LOADER_DETAILS, null, this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_retry:
                mReplyButtonVisible = false;
                startDetailService();
                break;

            case R.id.button_bar_chart:
                if (mExtrasInfo.getVisibility() == View.VISIBLE) {
                    Intent barChartIntent = new Intent(getActivity(), BarChartActivity.class);
                    barChartIntent.setData(mDetailUri);
                    startActivity(barChartIntent);
                } else {
                    showBarChartButtonToast();
                }
                break;
        }
    }

    /**
     * Starts the a {@link DetailService} to perform a network request to retrieve the symbol's
     * history.
     */
    private void startDetailService() {
        mIsDetailRequestLoading = true;
        showProgressWheel();

        Intent serviceIntent = new Intent(getActivity(), DetailService.class);
        serviceIntent.putExtra(Constants .KEY_DETAIL_SYMBOL,
                StockContract.getSymbolFromUri(mDetailUri));

        getActivity().startService(serviceIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                mDetailUri,
                Constants.DETAIL_PROJECTION,
                null,
                null,
                null);
    }

    // This function is also guaranteed to be called prior to the release of the last data that was
    // supplied for this Loader." During onResume it is perfectly reasonable that the loader
    // releases its data and reloads during onResume. Yes, if you are seeing a behavior where the
    // loader may callback and you don't want that callback, then destroy the loader.
    // http://stackoverflow.com/questions/21031692/why-is-onloadfinished-called-again-after-fragment-resumed
    // I destroy the Loader when I finished getting the extras section, so it doesn't happen.
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            int prevStreak = data.getInt(Constants.INDEX_PREV_STREAK);

            // Set update time
            Date lastUpdate = Utility.getLastUpdateTime(getActivity().getContentResolver()).getTime();
            SimpleDateFormat sdf = new SimpleDateFormat(mUpdateTimeFormat, Locale.US);
            String lastUpdateString = getString(R.string.placeholder_update_time, sdf.format(lastUpdate));

            // Main Section
            // Add check here so when the service returns from calculating the prev streak info
            // it wont have to load main section again.
            if (!mTextUpdateTime.getText().toString().equals(lastUpdateString)) {
                initMainSection(data, lastUpdateString);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().startPostponedEnterTransition();
                }
            }
            // Extras Section
            if (prevStreak != 0) {
                initExtrasSection(data, prevStreak);

            } else if (mReplyButtonVisible) {
                showRetryButton();

            } else if (!mIsDetailRequestLoading) {
                startDetailService();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoadDetailFinished(LoadDetailFinishedEvent event) {
        /* Make sure we don't process the event of another stock symbol. This can happen is we
        switch to a different DetailFragment while the prev one is still loading. */
        if (event.getSymbol().equals(getSymbol())) {
            mIsDetailRequestLoading = false;
            if (!event.isSuccessful()) {
                showRetryButton();
            } else {
                showExtrasInfo();
            }
        }
        EventBus.getDefault().removeStickyEvent(LoadDetailFinishedEvent.class);
    }

    private void initMainSection(Cursor data, String lastUpdateString) {
        Log.d(TAG,"initMainSection");
       /* ButterKnife is awesome API */
        TextView tSymbol = ButterKnife.findById(mMainInfo, R.id.text_symbol);
        TextView tFullName = ButterKnife.findById(mMainInfo, R.id.text_full_name);
        TextView tRecentClose = ButterKnife.findById(mMainInfo, R.id.text_recent_close);
        TextView tChangeDollar = ButterKnife.findById(mMainInfo, R.id.text_change_dollar);
        TextView tChangePercent = ButterKnife.findById(mMainInfo, R.id.text_change_percent);
        TextView tStreak = ButterKnife.findById(mMainInfo, R.id.text_streak);
        TextView tChangePercentNegSign = ButterKnife.findById(mMainInfo, R.id.text_change_percent_neg_sign);
        ImageView iStreakArrow = ButterKnife.findById(mMainInfo, R.id.image_streak_arrow);
        TextView tStreakNegSign = ButterKnife.findById(mMainInfo, R.id.text_streak_neg_sign);

        float changePercent = data.getFloat(Constants.INDEX_CHANGE_PERCENT);
        int streak = data.getInt(Constants.INDEX_STREAK);
        mTextUpdateTime.setText(lastUpdateString);

        tSymbol.setText(data.getString(Constants.INDEX_SYMBOL));
        tFullName.setText(data.getString(Constants.INDEX_FULL_NAME));

        tRecentClose.setText(getString(R.string.placeholder_dollar,
                Utility.roundTo2StringDecimals(data.getFloat(Constants.INDEX_RECENT_CLOSE))));

        if (streak < 0) {
            tStreakNegSign.setVisibility(View.VISIBLE);
        }
        tStreak.setText(getString(Math.abs(streak) == 1 ?
                R.string.placeholder_day : R.string.placeholder_days, Math.abs(streak)));

        // Get our dollar/percent change colors and set our stock arrow ImageView
        // Determine the color and the arrow image of the changes
        Pair<Integer, Integer> changeColorAndDrawableIds = Utility.getChangeColorAndArrowDrawableIds(streak);

        int color = ContextCompat.getColor(getActivity(), changeColorAndDrawableIds.first);
        iStreakArrow.setBackgroundResource(changeColorAndDrawableIds.second);

        tChangeDollar.setText(getString(R.string.placeholder_dollar,
                Utility.roundTo2StringDecimals(data.getFloat(Constants.INDEX_CHANGE_DOLLAR))));

        if (changePercent < 0) {
            tChangePercentNegSign.setVisibility(View.VISIBLE);
            tChangePercentNegSign.setTextColor(color);
        }
        tChangePercent.setText(getString(R.string.placeholder_percent,
                Utility.roundTo2StringDecimals(Math.abs(changePercent))));

        tChangeDollar.setTextColor(color);
        tChangePercent.setTextColor(color);
    }

    private void initExtrasSection(Cursor data, int prevStreak) {
        Log.d(TAG,"initExtrasSection");
        TextView tPrevStreakEndPrice= ButterKnife.findById(mExtrasInfo, R.id.text_prev_streak_end_price);
        TextView tStreakYearHigh= ButterKnife.findById(mExtrasInfo, R.id.text_streak_year_high);
        TextView mStreakYearLow= ButterKnife.findById(mExtrasInfo, R.id.text_streak_year_low);
        ImageView iPrevStreakArrow= ButterKnife.findById(mExtrasInfo, R.id.image_prev_streak_arrow);
        TextView tPrevStreakNegSign= ButterKnife.findById(mExtrasInfo, R.id.text_streak_year_low);

        int streakYearHigh = data.getInt(Constants.INDEX_STREAK_YEAR_HIGH);
        int streakYearLow = data.getInt(Constants.INDEX_STREAK_YEAR_LOW);

        mIsDetailRequestLoading = false;

        if (prevStreak < 0) {
            tPrevStreakNegSign.setVisibility(View.VISIBLE);
        }
        mTextPrevStreak.setText(getString(Math.abs(prevStreak) == 1 ?
                R.string.placeholder_day : R.string.placeholder_days, Math.abs(prevStreak)));

        Pair<Integer, Integer> changeColorAndDrawableIds =
                Utility.getChangeColorAndArrowDrawableIds(prevStreak);
        iPrevStreakArrow.setBackgroundResource(changeColorAndDrawableIds.second);

        tPrevStreakEndPrice.setText(getString(R.string.placeholder_dollar,
                Utility.roundTo2StringDecimals(data.getFloat(Constants.INDEX_PREV_STREAK_END_PRICE))));

        tStreakYearHigh.setText(getString(Math.abs(streakYearHigh) == 1 ?
                R.string.placeholder_day : R.string.placeholder_days, streakYearHigh));

        mStreakYearLow.setText(getString(Math.abs(streakYearLow) == 1 ?
                R.string.placeholder_day : R.string.placeholder_days, Math.abs(streakYearLow)));

        showExtrasInfo();
        getActivity().getSupportLoaderManager().destroyLoader(Constants.ID_LOADER_DETAILS);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(Constants.KEY_REPLY_BUTTON_VISIBLE, mReplyButtonVisible);
        outState.putBoolean(Constants.KEY_IS_DETAIL_REQUEST_LOADING, mIsDetailRequestLoading);
        super.onSaveInstanceState(outState);
    }

    private void showBarChartButtonToast() {
        if (mBarChartButtonToast == null) {
            mBarChartButtonToast = Toast.makeText(getActivity(), R.string.toast_chart_data_not_yet_available, Toast.LENGTH_SHORT);
            mBarChartButtonToast.show();

        } else if (!mBarChartButtonToast.getView().isShown()) {
            mBarChartButtonToast.show();
        }
    }

    private void showProgressWheel() {
        mProgressWheel.setVisibility(View.VISIBLE);
        mRetryButton.setVisibility(View.INVISIBLE);
        mExtrasInfo.setVisibility(View.INVISIBLE);
    }

    private void showRetryButton() {
        mReplyButtonVisible = true;
        mProgressWheel.setVisibility(View.INVISIBLE);
        mRetryButton.setVisibility(View.VISIBLE);
        mExtrasInfo.setVisibility(View.INVISIBLE);
    }

    private void showExtrasInfo() {
        mProgressWheel.setVisibility(View.INVISIBLE);
        mRetryButton.setVisibility(View.INVISIBLE);
        mExtrasInfo.setVisibility(View.VISIBLE);
    }

    public String getSymbol() {
        return StockContract.getSymbolFromUri(mDetailUri);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
