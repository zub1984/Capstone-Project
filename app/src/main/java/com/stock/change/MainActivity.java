package com.stock.change;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.library.search.SearchBox;
import com.library.search.SearchResult;
import com.stock.change.adapters.MainAdapter;
import com.stock.change.custom.MyApplication;
import com.stock.change.custom.MyLinearLayoutManager;
import com.stock.change.data.ListEventQueue;
import com.stock.change.data.ListManipulator;
import com.stock.change.data.StockContract;
import com.stock.change.data.StockContract.StockEntry;
import com.stock.change.events.AppRefreshFinishedEvent;
import com.stock.change.events.InitLoadFromDbFinishedEvent;
import com.stock.change.events.LoadMoreFinishedEvent;
import com.stock.change.events.LoadSymbolFinishedEvent;
import com.stock.change.events.MainProgressWheelHideEvent;
import com.stock.change.events.MainProgressWheelShowEvent;
import com.stock.change.fragments.DetailEmptyFragment;
import com.stock.change.fragments.DetailFragment;
import com.stock.change.fragments.ListManagerFragment;
import com.stock.change.fragments.dialogs.AboutDialog;
import com.stock.change.fragments.dialogs.FaqDialog;
import com.stock.change.fragments.dialogs.MessageOfDayFragment;
import com.stock.change.fragments.dialogs.SortDialog;
import com.stock.change.services.MainService;
import com.stock.change.utils.Constants;
import com.stock.change.utils.Utility;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SearchBox.SearchListener,
        MainAdapter.EventListener, SwipeRefreshLayout.OnRefreshListener,
        ListManagerFragment.EventListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.text_empty_list)
    TextView mEmptyMsg;
    @BindView(R.id.coordinator_layout)
    CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @Nullable
    @BindView(R.id.detail_container)
    FrameLayout mDetailContainer;
    @BindView(R.id.search_box_logo)
    TextView mSearchLogo;
    @BindView(R.id.overflow)
    ImageView mOverflowMenuButton;
    @BindView(R.id.progress_wheel)
    ProgressBar mProgressWheel;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.edit_text_search)
    EditText mSearchEditText;
    @BindView(R.id.swipe_to_refresh)
    SwipeRefreshLayout mSwipeToRefresh;
    @BindView(R.id.search_box)
    SearchBox mToolbar;


    private MyLinearLayoutManager mLayoutManager;
    private MainAdapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewDragDropManager mDragDropManager;
    private RecyclerViewSwipeManager mSwipeManager;
    private RecyclerViewTouchActionGuardManager mTouchActionGuardManager;
    private Snackbar mSnackBar;

    private ListManagerFragment mListFragment;
    private PopupWindow mOverflowMenuPopUp;
    private View.OnClickListener mOverflowItemListener;
    private Toast mListUpToDateToast;

    private boolean mFirstOpen;
    private boolean mStartedFromWidget;
    private boolean mDragClickPreventionEnabled;
    private boolean mDynamicScrollLoadEnabled;
    private boolean mDynamicScrollLoadAnother;
    private int mNumberOfLaunchItems;
    private int mSelectedDrawerMenuId;

    private InterstitialAd mInterstitialAd;
    private int mItemClicksForInterstitial;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Init savedInstanceState first as many components rely on it
        initSavedInstanceState(savedInstanceState);
        setupDrawerMenuContent();
        //default it set first item as selected
        mSelectedDrawerMenuId = savedInstanceState == null ? R.id.navigation_home : savedInstanceState.getInt("SELECTED_ID");

        initOverflowMenu();
        initRecyclerView();
        initBannerAd();
        initInterstitialAd();
        initTagManagerAndAnalytics();

        mListFragment.setEventListener(this);
        mSwipeToRefresh.setOnRefreshListener(this);
        mToolbar.setSearchListener(this);

        mNumberOfLaunchItems = getResources().getInteger(R.integer.numberOfLaunchItems);
    }

    /**
     * Initializes variables depending on the state of savedInstanceState
     *
     * @param savedInstanceState of the activity
     */
    private void initSavedInstanceState(Bundle savedInstanceState) {
        // When Android kills this app because of low memory check if sessionId is empty. If it is,
        // start the app as if it were the first time.
        if (MyApplication.getInstance().getSessionId().isEmpty()) {
            savedInstanceState = null;
        }

        if (savedInstanceState == null) {
            mFirstOpen = true;
            // We have to generate a new session so network calls from previous sessions
            // have a chance to cancel themselves
            MyApplication.startNewSession();

            // Initialize the fragment that stores the list
            mListFragment = new ListManagerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(mListFragment, ListManagerFragment.TAG).commit();

            // Execute pending transaction to immediately add the ListManagerFragment because
            // the RecyclerView Adapter is dependent on it.
            getSupportFragmentManager().executePendingTransactions();

            // Checks to see if app is opened from widget
            Uri detailUri = getIntent().getData();
            if (detailUri != null) {
                mStartedFromWidget = true;
                String symbol = StockContract.getSymbolFromUri(detailUri);

                if (mDetailContainer != null) {
                    insertFragmentIntoDetailContainer(symbol);
                }
            }
            if (!mStartedFromWidget && mDetailContainer != null) {
                showDetailEmptyFragment();
            }

        } else { // saveInstanceState != null
            mListFragment = ((ListManagerFragment) getSupportFragmentManager()
                    .findFragmentByTag(ListManagerFragment.TAG));

            // If editText was focused, return that focus on orientation change
            if (savedInstanceState.getBoolean(Constants.KEY_SEARCH_FOCUSED)) {
                mToolbar.toggleSearch();

            } else if (!savedInstanceState.getBoolean(Constants.KEY_LOGO_VISIBLE)) {
                // Else if not focused, but logo is invisible, open search w/o the focus
                mToolbar.toggleSearch();
                mSearchEditText.clearFocus();
            }

            if (savedInstanceState.getBoolean(Constants.KEY_PROGRESS_WHEEL_VISIBLE)) {
                mProgressWheel.setVisibility(View.VISIBLE);

            } else if (savedInstanceState.getBoolean(Constants.KEY_EMPTY_MSG_VISIBLE)) {
                mEmptyMsg.setVisibility(View.VISIBLE);
            }

            mFirstOpen = savedInstanceState.getBoolean(Constants.KEY_FIRST_OPEN);
            mDynamicScrollLoadEnabled = savedInstanceState.getBoolean(Constants.KEY_DYNAMIC_SCROLL_ENABLED);
            mDynamicScrollLoadAnother = savedInstanceState.getBoolean(Constants.KEY_DYNAMIC_LOAD_ANOTHER);
            mItemClicksForInterstitial = savedInstanceState.getInt(Constants.KEY_ITEM_CLICKS_FOR_INTERSTITIAL);
        }
    }


    /**
     * initialize the banner ad.
     */
    private void initBannerAd() {
        mAdView = (AdView) findViewById(R.id.adView);

        /*In production you need to make sure that you removed addTestDevice() methods in order to
        render the live ads and start monetization.*/
        /*AdRequest adRequest = new AdRequest.Builder()
                // Check the LogCat to get your test device ID
                .addTestDevice("ED54102968E3196BE131999F32345B82")
                .build();
        mAdView.loadAd(adRequest);*/

        mAdView.loadAd(new AdRequest.Builder().build());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Checks to see if app is opened from widget
        // On Phone --> DetailActivity, onCreate null, no onNewIntent. Click back to MainActivity, onCreate null, onNewIntent called.
        // On Phone & Tablet --> MainActivity, onNewIntent called only if already opened. If not onCreate null only.
        //
        // If the parent activity has launch mode <singleTop>, or the up intent contains
        // FLAG_ACTIVITY_CLEAR_TOP, the parent activity is brought to the top of the stack, and
        // receives the intent through its onNewIntent() method.
        Uri detailUri = intent.getData();
        if (detailUri != null) {
            String symbol = StockContract.getSymbolFromUri(detailUri);

            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(symbol);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initDynamicScrollListener();

        EventBus eventBus = EventBus.getDefault();
        eventBus.register(mListFragment);
        eventBus.register(this);

        // Fetch the stock information
        fetchStockInfo();
    }

    /**
     * Grabs the stock information from the db when app is first opened and updates it if possible.
     */
    private void fetchStockInfo() {
        ListEventQueue listEventQueue = ListEventQueue.getInstance();

        if (mFirstOpen) {
            mFirstOpen = false;
            mDynamicScrollLoadAnother = false;

            showProgressWheel();
            listEventQueue.clearQueue();
            mListFragment.initFromDb();

        } else {
            listEventQueue.postAllFromQueue();
            refreshList(null, false);
        }
    }

    /**
     * Sends a request to refresh the list.
     *
     * @param attachSymbol      The symbol to load after the list has been refreshed. This is to ensure
     *                          already loaded symbols will be in sync with the new symbol.
     * @param showUpToDateToast set to true if you would like a toast to notify if you're already up to
     *                          date.
     * @return true if list can be refreshed, false otherwise.
     */
    private boolean refreshList(String attachSymbol, boolean showUpToDateToast) {
        if (MyApplication.getInstance().isRefreshing()) {
            showProgressWheel();
            return true;

        } else if (Utility.canUpdateList(getContentResolver()) && mEmptyMsg.getVisibility() != View.VISIBLE) {
            // We set Dynamic scroll to false here as early as possible as a precaution to prevent
            // load as we don't setRefreshing to true until initFromRefresh();
            mDynamicScrollLoadEnabled = false;
            mDynamicScrollLoadAnother = false;

            // Dismiss Snack-bar to prevent undo removal because the old data will not be in sync with
            // new data when list is refreshed.
            if (mSnackBar != null && mSnackBar.isShown()) {
                mSnackBar.dismiss();
            }
            mListFragment.initFromRefresh(attachSymbol);
            return true;

        } else if (showUpToDateToast) {
            showListUpToDateToast();
        }

        return false;
    }

    @Override // SwipeRefreshLayout.OnRefreshListener
    public void onRefresh() {
        mSwipeToRefresh.setRefreshing(false);
        refreshList(null, true);
    }

    @Override // SearchBox.SearchListener
    public void onSearchOpened() {

    }

    @Override // SearchBox.SearchListener
    public void onSearchCleared() {

    }

    @Override // SearchBox.SearchListener
    public void onSearchClosed() {

    }

    @Override // SearchBox.SearchListener
    public void onSearchTermChanged(String term) {

    }

    @Override // SearchBox.SearchListener
    public void onResultClick(SearchResult result) {

    }

    @Override // SearchBox.SearchListener
    public void onSearch(String query) {
        if (!getListManipulator().isListLimitReached(MainActivity.this)) {
            query = query.toUpperCase(Locale.US);

            if (TextUtils.isEmpty(query)) {
                Toast.makeText(this, R.string.toast_empty_search, Toast.LENGTH_SHORT).show();

            } else if (!refreshList(query, false)) {
                // Refresh the shownList BEFORE fetching a new stock. This is to prevent
                // fetching the new stock twice when it becomes apart of that list.
                mListFragment.loadSymbol(query);
            }
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadFromDbFinished(InitLoadFromDbFinishedEvent event) {
        hideProgressWheel();
        mAdapter.notifyDataSetChanged();

        if (getListManipulator().getCount() == 0) {
            showEmptyMessage();

        } else if (mDetailContainer != null && !mStartedFromWidget) {
            // Load the first item into the container, when db finishes loading
            insertFragmentIntoDetailContainer(getListManipulator().getItem(0).getSymbol());
        }

        if (!refreshList(null, false) && getListManipulator().getCount() < mNumberOfLaunchItems) {
            dynamicLoadMore();
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadSymbolFinished(LoadSymbolFinishedEvent event) {
        if (event.isSuccessful()) {
            if (!mDragDropManager.isDragging() && !mSwipeManager.isSwiping()) {
                // Can't use notifyDataInserted(...) because we are suppose to update adapter size
                // AND notifyItem...() in the main thread and same call stack. We happen to update
                // adapter size in a bg thread. Also another problem is when we rotate and we post
                // all from queue, we will call notifyItemInserted w/o inserting an item.
                // But we can use notifyDataSetChanged() to work around that.
                // http://stackoverflow.com/questions/30220771/recyclerview-inconsistency-detected-invalid-item-position
                mAdapter.notifyDataSetChanged();
                mRecyclerView.smoothScrollToPosition(0);
            }
            mSearchEditText.setText("");

            // If tablet, insert fragment into container
            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(getListManipulator().getItem(0).getSymbol());
            }

            // Send to Tag Manager a hit of a Symbol Add Event.
            sendSymbolAddHit(event.getStock().getSymbol());

        } else {
            if (getListManipulator().getCount() == 0) {
                showEmptyWidgets();
            }
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onLoadMoreFinished(LoadMoreFinishedEvent event) {
        if (event.isSuccessful()) {
            if (!mDragDropManager.isDragging() && !mSwipeManager.isSwiping()) {
                mAdapter.notifyDataSetChanged();
            }

            // Load more only if number of launch items are not reached or if we have the signal to
            // load another
            if (getListManipulator().getCount() < mNumberOfLaunchItems || mDynamicScrollLoadAnother) {
                dynamicLoadMore();
                mDynamicScrollLoadAnother = false;
            } else {
                mDynamicScrollLoadEnabled = true;
            }

        } else {
            //Show retry button
            if (!mDragDropManager.isDragging() && !mSwipeManager.isSwiping()) {
                mAdapter.notifyItemChanged(getListManipulator().getCount() - 1);
            }
        }
    }

    @Override // ListManipulatorFragment.EventListener
    public void onRefreshFinished(AppRefreshFinishedEvent event) {
        if (event.isSuccessful()) {
            mAdapter.notifyDataSetChanged();
            dynamicLoadMore();

            // If tablet, insert first item's fragment into container
            if (mDetailContainer != null) {
                insertFragmentIntoDetailContainer(getListManipulator().getItem(0).getSymbol());
            }

        } else {
            if (getListManipulator().getCount() == 0) {
                showEmptyWidgets();
            }
        }
    }

    @Override // MainAdapter.EventListener
    public void onStockItemClick(MainAdapter.MainViewHolder holder) {
        if (!mDragClickPreventionEnabled) {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                String symbol = getListManipulator().getItem(position).getSymbol();

                //If tablet insert fragment into container
                if (mDetailContainer != null) {
                    String detailSymbol = ((DetailFragment) getSupportFragmentManager()
                            .findFragmentByTag(DetailFragment.TAG)).getSymbol();

                    // If the symbol's detail fragment is already loaded, don't load again if
                    // clicked again.
                    if (!detailSymbol.equals(symbol)) {
                        insertFragmentIntoDetailContainer(symbol);
                    }
                } else {
                    insertFragmentIntoDetailActivity(symbol);
                }

                // Check if it is time to show interstitial ad
                checkItemClickAdCondition();
            }
        }
    }

    @Override // MainAdapter.EventListener
    public void onStockItemRemoved(MainAdapter.MainViewHolder holder) {
        int position = holder.getAdapterPosition();

        if (position != RecyclerView.NO_POSITION) {
            final ListManipulator listManipulator = getListManipulator();
            String removeSymbol = listManipulator.getItem(position).getSymbol();
            // Delete the lastRemovedItem before, removing another item. Can't rely on Snackbar's
            // onDismissed() callback because it only gets called AFTER this method is finished.
            listManipulator.permanentlyDeleteLastRemoveItem(MainActivity.this);
            listManipulator.removeItem(position);

            if (listManipulator.getCount() == 0) {
                showEmptyWidgets();

            } else if (mDetailContainer != null) {
                // Show the first item in the detail container if the one being removed is
                // currently in the detail container.
                String detailSymbol = ((DetailFragment) getSupportFragmentManager()
                        .findFragmentByTag(DetailFragment.TAG)).getSymbol();

                if (detailSymbol.equals(removeSymbol)) {
                    insertFragmentIntoDetailContainer(listManipulator.getItem(0).getSymbol());
                }
            }

            // Snack-bar to give the option to undo the removal of an item or else it will
            // permanently delete from database once it is dismissed.
            // Note: Snack-bar onDismiss callback follow weird order of execution. It doesn't get
            // called when you expect it to.
            mSnackBar = Snackbar.make(
                    mCoordinatorLayout,
                    getString(R.string.placeholder_snackbar_main_text, removeSymbol), Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_action_text, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!getListManipulator().isListLimitReached(MainActivity.this)) {
                                hideEmptyMessage();

                                int undoPosition = listManipulator.undoLastRemoveItem();
                                mAdapter.notifyItemInserted(undoPosition);
                                mRecyclerView.smoothScrollToPosition(undoPosition);

                                if (mDetailContainer != null) {
                                    insertFragmentIntoDetailContainer(
                                            listManipulator.getItem(undoPosition).getSymbol());
                                }
                            }
                        }
                    }).setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION
                                    && event != Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE) {
                                listManipulator.permanentlyDeleteLastRemoveItem(MainActivity.this);
                            }
                        }
                    });
            mSnackBar.show();

        }
    }

    @Override // MainAdapter.EventListener
    public void onStockItemMoved(int fromPosition, int toPosition) {
        getListManipulator().moveItem(fromPosition, toPosition);
    }

    @Override // MainAdapter.EventListener
    public void onStockItemTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDragClickPreventionEnabled = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mDragDropManager != null && !mDragDropManager.isDragging()) {
                    mDragDropManager.cancelDrag();
                }
                break;
        }
    }

    @Override // MainAdapter.EventListener
    public void onLoadItemRetryClick(MainAdapter.LoadViewHolder holder) {
        if (Utility.canUpdateList(getContentResolver())) {
            Toast.makeText(this, R.string.toast_list_out_of_sync, Toast.LENGTH_SHORT).show();

        } else if (!MyApplication.getInstance().isRefreshing()) {
            mListFragment.loadMore();
            mAdapter.notifyItemChanged(getListManipulator().getCount() - 1);
        }
    }


    /**
     * setup the navigation drawer menu.
     */
    private void setupDrawerMenuContent() {
        final NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        mToolbar.setMenuListener(new SearchBox.MenuListener() {
            @Override
            public void onMenuClick() {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    /**
     * Display the dialog message on menu item selection.
     *
     * @param menuItem selected menu item
     */
    public void selectDrawerItem(MenuItem menuItem) {
        menuItem.setChecked(true);
        mSelectedDrawerMenuId = menuItem.getItemId();
        switch (mSelectedDrawerMenuId) {
            case R.id.navigation_msg_of_the_day:
                new MessageOfDayFragment().show(getSupportFragmentManager(), MessageOfDayFragment.TAG);
                break;

            case R.id.navigation_faq:
                new FaqDialog().show(getSupportFragmentManager(), FaqDialog.TAG);
                break;

            case R.id.navigation_about:
                new AboutDialog().show(getSupportFragmentManager(), AboutDialog.TAG);
                break;

            case R.id.navigation_google_play:
                // Link to the app in the play store
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (ActivityNotFoundException e) {
                    MyApplication.getInstance().trackException(e);
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                break;
        }

        // Delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }
        }, Constants.NAV_DRAWER_CLOSE_DELAY);
    }


    /**
     * Initializes the overflow menu.
     */
    private void initOverflowMenu() {
        mToolbar.setOverflowMenuClickLister(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);

                View overflowLayout = layoutInflater.inflate(R.layout.overflow_menu_custom, null);
                mOverflowMenuPopUp = new PopupWindow(overflowLayout,
                        getResources().getDimensionPixelSize(R.dimen.overflow_menu_width),
                        ViewGroup.LayoutParams.WRAP_CONTENT, true);

                // A workaround to dismiss PopUpWindow to be dismissed when touched outside.
                mOverflowMenuPopUp.setBackgroundDrawable(new ColorDrawable());

                int cardViewCompatPaddingVerticalOffset =
                        getResources().getDimensionPixelSize(R.dimen.overflow_menu_compat_padding);
                // Horizontal Offset is 0 because no matter how much we offset it will not offset any
                // more to the right since we set compat padding to true on the
                // menu card view. That extra padding will not go off screen since it is part of the
                // menu now and Android wants to fit the entire view on screen.
                mOverflowMenuPopUp.showAsDropDown(mOverflowMenuButton, 0, -cardViewCompatPaddingVerticalOffset);

                overflowLayout.findViewById(R.id.overflow_refresh).setOnClickListener(mOverflowItemListener);
                overflowLayout.findViewById(R.id.overflow_sort).setOnClickListener(mOverflowItemListener);
            }
        });

        // Setup overflow menu clicks here
        mOverflowItemListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.overflow_refresh:
                        refreshList(null, true);
                        break;

                    case R.id.overflow_sort:
                        SortDialog.newInstance(new SortDialog.OnSortFinishedListener() {
                            @Override
                            public void onSortFinished() {
                                mAdapter.notifyDataSetChanged();
                            }
                        }).show(getSupportFragmentManager(), SortDialog.TAG);
                        break;
                }

                mOverflowMenuPopUp.dismiss();
            }
        };
    }

    /**
     * Initializes the {@link RecyclerView}.
     */
    private void initRecyclerView() {
        // Touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mTouchActionGuardManager.setEnabled(true);

        // Drag & drop manager
        mDragDropManager = new RecyclerViewDragDropManager();
        mDragDropManager.setDraggingItemShadowDrawable(
                (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.material_shadow_z3));
        // Start dragging after long press
        mDragDropManager.setInitiateOnLongPress(true);
        mDragDropManager.setInitiateOnMove(false);
        mDragDropManager.setOnItemDragEventListener(new RecyclerViewDragDropManager.OnItemDragEventListener() {
            @Override
            public void onItemDragStarted(int position) {
                mDragClickPreventionEnabled = true;
            }

            @Override
            public void onItemDragPositionChanged(int fromPosition, int toPosition) {
            }

            @Override
            public void onItemDragFinished(int fromPosition, int toPosition, boolean result) {
                mAdapter.notifyDataSetChanged();
            }
        });

        // Swipe manager
        mSwipeManager = new RecyclerViewSwipeManager();
        mSwipeManager.setOnItemSwipeEventListener(new RecyclerViewSwipeManager.OnItemSwipeEventListener() {
            @Override
            public void onItemSwipeStarted(int position) {
            }

            @Override
            public void onItemSwipeFinished(int position, int result, int afterSwipeReaction) {
                mAdapter.notifyDataSetChanged();
            }
        });

        //Create adapter
        final MainAdapter mainAdapter = new MainAdapter(this, this, mListFragment);

        mAdapter = mainAdapter;
        mWrappedAdapter = mDragDropManager.createWrappedAdapter(mainAdapter);  // Wrap for dragging
        mWrappedAdapter = mSwipeManager.createWrappedAdapter(mWrappedAdapter); // Wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22. Disable the
        // change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);
        mLayoutManager = new MyLinearLayoutManager(this);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mWrappedAdapter);
        mRecyclerView.setItemAnimator(animator);
        mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(
                ContextCompat.getDrawable(this, R.drawable.list_divider_h), true));

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mTouchActionGuardManager.attachRecyclerView(mRecyclerView);
        mSwipeManager.attachRecyclerView(mRecyclerView);
        mDragDropManager.attachRecyclerView(mRecyclerView);
    }

    /**
     * Initializes the RecyclerView's Scroll Listener that changes elevation of the Toolbar
     * depending on the scroll offset. Also it will attempt to load more data when the last item
     * if the list is shown.
     */
    private void initDynamicScrollListener() {
        // When recyclerView is scrolled all the way to the top, appbar elevation will disappear.
        // When you start scrolling down elevation will reappear.

        // This gets called on instantiation, on item add, and on scroll
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mDetailContainer == null) {
                    if (recyclerView.computeVerticalScrollOffset() <= 2) { //0 or 1 not reliable
                        mToolbar.setElevation(0);
                    } else {
                        mToolbar.setElevation(
                                getResources().getDimension(R.dimen.appbar_elevation));
                    }
                }
                if (mDynamicScrollLoadEnabled
                        && mLayoutManager.findLastVisibleItemPosition() == getListManipulator().getCount() - 1) {
                    dynamicLoadMore();
                }
            }
        });
    }

    /**
     * Loads more data to the bottom of your list.
     */
    private void dynamicLoadMore() {
        ListManipulator listManipulator = getListManipulator();

        if (!listManipulator.isLoadingItemPresent()
                && !MyApplication.getInstance().isRefreshing()
                && listManipulator.canLoadMore()) {
            // Insert loading item
            listManipulator.addLoadingItem();

            if (!Utility.canUpdateList(getContentResolver())) {
                mListFragment.loadMore();
            } else {
                Toast.makeText(this, R.string.toast_list_out_of_sync, Toast.LENGTH_SHORT).show();
            }

            // Must notifyItemInserted AFTER loadMore for mIsLoadingAFew to be updated
            if (!mDragDropManager.isDragging() && !mSwipeManager.isSwiping()) {
                mAdapter.notifyItemInserted(listManipulator.getCount() - 1);
            }

            // When dynamic scroll is enabled and it load we will load twice. We immediately disable
            // dynamic scroll as a precaution to prevent overlapping of loads. We will re-enable
            // after callback is called.
            if (mDynamicScrollLoadEnabled) {
                mDynamicScrollLoadAnother = true;
                mDynamicScrollLoadEnabled = false;
            }
        }
    }

    /**
     * Inserts a {@link DetailFragment} containing the symbol's details into the Detail Container.
     *
     * @param symbol The symbol to insert the fragment for.
     */
    private void insertFragmentIntoDetailContainer(@NonNull String symbol) {
        Uri detailUri = StockEntry.buildUri(symbol);
        Bundle args = new Bundle();
        args.putParcelable(Constants.KEY_DETAIL_URI, detailUri);

        DetailFragment detailFragment = new DetailFragment();
        detailFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_container, detailFragment, DetailFragment.TAG)
                .commit();
    }

    /**
     * Inserts a {@link DetailFragment} containing the symbol's details into the
     * {@link DetailActivity}.
     *
     * @param symbol The symbol to insert the fragment for.
     */
    private void insertFragmentIntoDetailActivity(@NonNull String symbol) {
        mSearchEditText.clearFocus();
        Uri detailUri = StockEntry.buildUri(symbol);
        //If phone open activity
        Intent openDetail = new Intent(MainActivity.this, DetailActivity.class);
        openDetail.setData(detailUri);

        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle();
        startActivity(openDetail, bundle);
    }

    public ListManipulator getListManipulator() {
        if (mListFragment != null) {
            return mListFragment.getListManipulator();
        }
        return null;
    }

    private void showListUpToDateToast() {
        if (mListUpToDateToast == null) {
            mListUpToDateToast = Toast.makeText(this, R.string.toast_list_is_up_to_date, Toast.LENGTH_SHORT);
            mListUpToDateToast.show();

        } else if (!mListUpToDateToast.getView().isShown()) {
            mListUpToDateToast.show();
        }
    }

    private void showEmptyWidgets() {
        showEmptyMessage();
        if (mDetailContainer != null) {
            showDetailEmptyFragment();
        }
    }

    private void showDetailEmptyFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_container, new DetailEmptyFragment(), DetailFragment.TAG)
                .commit();
    }

    private void showEmptyMessage() {
        if (mProgressWheel.getVisibility() == View.INVISIBLE) {
            mEmptyMsg.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyMessage() {
        mEmptyMsg.setVisibility(View.INVISIBLE);
    }

    private void showProgressWheel() {
        mEmptyMsg.setVisibility(View.INVISIBLE);
        mProgressWheel.setVisibility(View.VISIBLE);
    }

    /**
     * This method is reserved for operations not on the network, but still require a progress wheel.
     * {@link MainService#onDestroy()} will cover hiding network operation progress wheels.
     */
    private void hideProgressWheel() {
        if (!Utility.isServiceRunning((ActivityManager) getSystemService(ACTIVITY_SERVICE),
                MainService.class.getName())) {
            mProgressWheel.setVisibility(View.INVISIBLE);
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMainProgressWheelShow(MainProgressWheelShowEvent event) {
        EventBus.getDefault().removeStickyEvent(MainProgressWheelShowEvent.class);
        showProgressWheel();
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMainProgressWheelHide(MainProgressWheelHideEvent event) {
        EventBus.getDefault().removeStickyEvent(MainProgressWheelHideEvent.class);
        mProgressWheel.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(Constants.KEY_SEARCH_FOCUSED, mSearchEditText.isFocused());
        outState.putBoolean(Constants.KEY_LOGO_VISIBLE, mSearchLogo.getVisibility() == View.VISIBLE);
        outState.putBoolean(Constants.KEY_PROGRESS_WHEEL_VISIBLE, mProgressWheel.getVisibility() == View.VISIBLE);
        outState.putBoolean(Constants.KEY_EMPTY_MSG_VISIBLE, mEmptyMsg.getVisibility() == View.VISIBLE);
        outState.putBoolean(Constants.KEY_FIRST_OPEN, mFirstOpen);
        outState.putBoolean(Constants.KEY_DYNAMIC_SCROLL_ENABLED, mDynamicScrollLoadEnabled);
        outState.putBoolean(Constants.KEY_DYNAMIC_LOAD_ANOTHER, mDynamicScrollLoadAnother);
        outState.putInt(Constants.KEY_ITEM_CLICKS_FOR_INTERSTITIAL, mItemClicksForInterstitial);
        outState.putInt(Constants.KEY_DRAWER_ITEM_ID, mSelectedDrawerMenuId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {

        if (mAdView != null) {
            mAdView.pause();
        }

        mDragDropManager.cancelDrag();
        if (mSnackBar != null && mSnackBar.isShown()) {
            mSnackBar.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onStop() {
        if (mRecyclerView != null) {
            mRecyclerView.clearOnScrollListeners();
        }
        EventBus eventBus = EventBus.getDefault();
        eventBus.unregister(mListFragment);
        eventBus.unregister(this);

        super.onStop();
    }

    @Override
    public void onDestroy() {

        if (mAdView != null) {
            mAdView.destroy();
        }

        if (mDragDropManager != null) {
            mDragDropManager.release();
            mDragDropManager = null;
        }

        if (mSwipeManager != null) {
            mSwipeManager.release();
            mSwipeManager = null;
        }

        if (mTouchActionGuardManager != null) {
            mTouchActionGuardManager.release();
            mTouchActionGuardManager = null;
        }

        if (mRecyclerView != null) {
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.setAdapter(null);
            mRecyclerView = null;
        }

        if (mWrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(mWrappedAdapter);
            mWrappedAdapter = null;
        }
        mAdapter.release();
        mAdapter = null;
        mLayoutManager = null;
        mListFragment = null;

        super.onDestroy();
    }

    /**
     * Send a hit to Analytics to track data of a Symbol Add Event.
     *
     * @param symbol hit by user
     */
    private void sendSymbolAddHit(String symbol) {
        MyApplication.getInstance().trackEvent(
                getString(R.string.analytics_category),
                getString(R.string.analytics_action_add),
                getString(R.string.analytics_label_add_placeholder, symbol)
        );
    }

    private void initTagManagerAndAnalytics() {
        // Init Analytics and Tag Manager in AsyncTask to slightly speed up app startup
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Init Analytics
                //MyApplication.getInstance().initAnalyticsTracking();
                // Init Tag Manager / GTM container
                TagManager tagManager = MyApplication.getInstance().getTagManager();

                // Retrieves a fresh SAVED container. I don't think it ever performs network operations..
                tagManager.loadContainerPreferFresh(getString(R.string.tag_manager_motd_container_id), R.raw.gtm_default_container).setResultCallback(new ResultCallback<ContainerHolder>() {
                    @Override
                    public void onResult(@NonNull ContainerHolder containerHolder) {
                        // Refresh container over network manually in case the "fresh" container is stale.
                        containerHolder.refresh();
                        // Store the containerHolder for message of the day.
                        MyApplication.getInstance().setContainerHolder(containerHolder);
                    }
                }, Constants.MOTD_CONTAINER_TIMEOUT, TimeUnit.MILLISECONDS);

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Initializes the interstitial ad.
     */
    private void initInterstitialAd() {
        mInterstitialAd = new InterstitialAd(this);
        // set the ad unit ID
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));

        // comment this method for production
        /*AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("ED54102968E3196BE131999F32345B82")
                .build();
        // Load ads into Interstitial Ads
        mInterstitialAd.loadAd(adRequest);*/

        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Toast.makeText(getApplicationContext(), "Ad is loaded!", Toast.LENGTH_SHORT).show();
                showInterstitial();
            }

            @Override
            public void onAdClosed() {
                Toast.makeText(getApplicationContext(), "Ad is closed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showInterstitial() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    /**
     * Requests a new interstitial ad to be loaded.
     */
    private void requestNewInterstitialAd() {
        // comment this method for production
      /*  AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("ED54102968E3196BE131999F32345B82")
                .build();
        // Load ads into Interstitial Ads
        mInterstitialAd.loadAd(adRequest);*/

        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    /**
     * check and display interstitial ad if divided  by Constants.CLICKS_UNTIL_INTERSTITIAL.
     */
    private void checkItemClickAdCondition() {
        mItemClicksForInterstitial++;
        if (mItemClicksForInterstitial % Constants.CLICKS_UNTIL_INTERSTITIAL == 0) {
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                requestNewInterstitialAd();
            }
        }
    }

}
