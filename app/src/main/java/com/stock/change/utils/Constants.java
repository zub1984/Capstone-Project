package com.stock.change.utils;

import com.stock.change.data.StockContract;

/**
 * Created by laptop on 8/5/2016.
 */
public class Constants {

    public static final String KEY_SEARCH_FOCUSED = "searchFocused";
    public static final String KEY_LOGO_VISIBLE = "logoVisible";
    public static final String KEY_PROGRESS_WHEEL_VISIBLE = "progressWheelVisible";
    public static final String KEY_EMPTY_MSG_VISIBLE = "emptyMsgVisible";

    public static final String KEY_FIRST_OPEN = "firstOpen";
    public static final String KEY_DYNAMIC_SCROLL_ENABLED = "dynamicScrollEnabled";
    public static final String KEY_DYNAMIC_LOAD_ANOTHER = "dynamicLoadAnother";
    public static final String KEY_ITEM_CLICKS_FOR_INTERSTITIAL = "ItemClicksForInterstitial";

    public static final int CLICKS_UNTIL_INTERSTITIAL = 10;
    public static final int MOTD_CONTAINER_TIMEOUT = 3000;
    public static final int NAV_DRAWER_CLOSE_DELAY = 200;


    // define the constants for detail fragment
    public static final int ID_LOADER_DETAILS = 2;
    public static final String KEY_REPLY_BUTTON_VISIBLE = "replyButtonVisible";
    public static final String KEY_IS_DETAIL_REQUEST_LOADING = "isDetailRequestLoading";
    public static final String KEY_DETAIL_URI = "detailUri";


    // define the projection for detail fragment
    public static final String[] DETAIL_PROJECTION = new String[]{
            StockContract.StockEntry.COLUMN_SYMBOL,
            StockContract.StockEntry.COLUMN_FULL_NAME,
            StockContract.StockEntry.COLUMN_RECENT_CLOSE,
            StockContract.StockEntry.COLUMN_CHANGE_DOLLAR,
            StockContract.StockEntry.COLUMN_CHANGE_PERCENT,
            StockContract.StockEntry.COLUMN_STREAK,
            StockContract.StockEntry.COLUMN_PREV_STREAK,
            StockContract.StockEntry.COLUMN_PREV_STREAK_END_PRICE,
            StockContract.StockEntry.COLUMN_STREAK_YEAR_HIGH,
            StockContract.StockEntry.COLUMN_STREAK_YEAR_LOW
    };

    //index must match projection
    public static final int INDEX_SYMBOL = 0;
    public static final int INDEX_FULL_NAME = 1;
    public static final int INDEX_RECENT_CLOSE = 2;
    public static final int INDEX_CHANGE_DOLLAR = 3;
    public static final int INDEX_CHANGE_PERCENT = 4;
    public static final int INDEX_STREAK = 5;
    public static final int INDEX_PREV_STREAK = 6;
    public static final int INDEX_PREV_STREAK_END_PRICE = 7;
    public static final int INDEX_STREAK_YEAR_HIGH = 8;
    public static final int INDEX_STREAK_YEAR_LOW = 9;

    // widgets action variables
    public static final String ACTION_DATA_REFRESH = StockContract.CONTENT_AUTHORITY + ".widget." + "DATA_REFRESH";
    public static final String ACTION_DATA_UPDATED = StockContract.CONTENT_AUTHORITY + ".widget." + "DATA_UPDATED";
    public static final String ACTION_DATA_UPDATE_ERROR = StockContract.CONTENT_AUTHORITY + ".widget." + "DATA_UPDATE_ERROR";

    // widget remote view variables
    public static final int MORE = 12;

    public static final String[] W_STOCK_PROJECTION = new String[]{
            StockContract.StockEntry.COLUMN_SYMBOL,
            StockContract.StockEntry.COLUMN_FULL_NAME,
            StockContract.StockEntry.COLUMN_RECENT_CLOSE,
            StockContract.StockEntry.COLUMN_CHANGE_DOLLAR,
            StockContract.StockEntry.COLUMN_CHANGE_PERCENT,
            StockContract.StockEntry.COLUMN_STREAK,
            StockContract.StockEntry.COLUMN_LIST_POSITION
    };

    //index must match projection
    public static final int W_INDEX_SYMBOL = 0;
    public static final int W_INDEX_FULL_NAME = 1;
    public static final int W_INDEX_RECENT_CLOSE = 2;
    public static final int W_INDEX_CHANGE_DOLLAR = 3;
    public static final int W_INDEX_CHANGE_PERCENT = 4;
    public static final int W_INDEX_STREAK = 5;
    public static final int W_INDEX_LIST_POSITION = 6;

    // main service constant variables
    public static final String KEY_LOAD_SYMBOL_QUERY = "searchQuery";
    public static final String KEY_LOAD_MORE_QUERY = "loadMoreQuery";
    public static final String KEY_SESSION_ID = "sessionId";

    public static final String ACTION_LOAD_MORE = "actionLoadMore";
    public static final String ACTION_LOAD_SYMBOL = "actionStockWithSymbol";
    public static final String ACTION_APP_REFRESH = "actionAppRefresh";
    public static final String ACTION_WIDGET_REFRESH = "actionWidgetRefresh";

    // bar chart projection variable
    public static final String[] CHART_PROJECTION = new String[]{
            StockContract.StockEntry.COLUMN_PREV_STREAK_END_DATE,
            StockContract.StockEntry.COLUMN_PREV_STREAK_END_PRICE,
            StockContract.StockEntry.COLUMN_STREAK
    };

    //indexes for the projection
    public static final int indexPrevStreakEndDate = 0;
    public static final int indexPrevStreakEndPrice = 1;
    public static final int indexStreak = 2;

    //details service
    public static final String KEY_DETAIL_SYMBOL = "detailSymbol";


}
