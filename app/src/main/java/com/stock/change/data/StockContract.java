package com.stock.change.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for the stock streaks database.
 */
public class StockContract {

    /**The "Content authority" is a name for the entire content provider, similar to the
    relationship between a domain name and its website.  A convenient string to use for the
    content authority is the package name for the app, which is guaranteed to be unique on the
    device.*/
    public static final String CONTENT_AUTHORITY = "com.stock.change";

    /** Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    the content provider.
    This full string will be: "content://com.stock.change" */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /* Possible paths (appended to base content URI for possible URI's)
    For instance, content://com.stock.change/stocks/ is a valid path for
    looking at stocks data. Simply put, these should be names of your tables! */
    public static final String PATH_STOCKS = "stocks";
    public static final String PATH_SAVE_STATE = "save_state";



    /** Inner class that defines the table contents of the update_date table */
    public static final class SaveStateEntry implements BaseColumns {
        // content://com.stock.change/save_state
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SAVE_STATE).build();

        // There will only be one item in the update date table and that is the time_in_milli
        // "vnd.android.cursor.item/com.stock.change/save_state
        public static final String CONTENT_DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
            + CONTENT_AUTHORITY + "/" + PATH_SAVE_STATE;

        public static final String TABLE_NAME = PATH_SAVE_STATE;
        public static final String COLUMN_UPDATE_TIME_IN_MILLI = "update_time_in_milli";
        public static final String COLUMN_SHOWN_POSITION_BOOKMARK = "shown_position_bookmark";
    }



    /** Inner class that defines the table contents of the stocks table */
    public static final class StockEntry implements BaseColumns {

        // content://com.stock.change/stocks
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_STOCKS).build();

        // "vnd.android.cursor.dir/com.stock.change/stocks
        public static final String CONTENT_DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
                + CONTENT_AUTHORITY + "/" + PATH_STOCKS;

        // "vnd.android.cursor.item/com.stock.change/stocks
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_STOCKS;

        public static final String TABLE_NAME = PATH_STOCKS;
        public static final String COLUMN_LIST_POSITION = "list_position";
        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_FULL_NAME = "full_name";
        public static final String COLUMN_RECENT_CLOSE = "recent_close";
        public static final String COLUMN_CHANGE_DOLLAR = "change_dollar";
        public static final String COLUMN_CHANGE_PERCENT = "change_percent";
        public static final String COLUMN_STREAK = "streak";
        public static final String COLUMN_PREV_STREAK_END_DATE = "prev_streak_end_date";
        public static final String COLUMN_PREV_STREAK_END_PRICE = "prev_streak_end_price";
        public static final String COLUMN_PREV_STREAK= "prev_streak";
        public static final String COLUMN_STREAK_YEAR_HIGH = "streak_year_high";
        public static final String COLUMN_STREAK_YEAR_LOW = "streak_year_low";
        public static final String COLUMN_STREAK_CHART_MAP_CSV = "streak_chart_map_csv";

        /**
         * This will serve as the return URI for something like inserting a row.
         *
         * @param symbol The symbol of the stock
         * @return The URI of the row of the recent transaction.
         * e.g. content://com.stock.change/stocks/GPRO
         */
        public static Uri buildUri(String symbol) {
            return CONTENT_URI.buildUpon().appendPath(symbol).build();
        }
    }

    /**
     * Extracts the symbol from the URI
     * @param uri The URI to extract the symbol from.
     * @return
     */
    public static String getSymbolFromUri(Uri uri){
        // content://com.stock.change/stocks/GPRO
        return uri.getPathSegments().get(1);
    }

    /**
     * @param uri
     * @return true if the uri is referring to the save state path, false otherwise.
     */
    public static boolean isSaveStateUri(Uri uri){
        // content://com.stock.change/save_state/
        return uri.getPathSegments().get(0).equals(PATH_SAVE_STATE);
    }
}
