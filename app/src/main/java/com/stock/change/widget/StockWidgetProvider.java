package com.stock.change.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.view.View;
import android.widget.RemoteViews;

import com.stock.change.DetailActivity;
import com.stock.change.MainActivity;
import com.stock.change.R;
import com.stock.change.custom.MyApplication;
import com.stock.change.services.MainService;
import com.stock.change.utils.Constants;
import com.stock.change.utils.Utility;

import java.util.Calendar;


public class StockWidgetProvider extends AppWidgetProvider {
    public static final String TAG = StockWidgetProvider.class.getSimpleName();

    @Override
    public void onEnabled(Context context) {
        // Start alarmManager
        Calendar stockMarketClose = Utility.getNewYorkCalendarQuickSetup(
                Utility.STOCK_MARKET_UPDATE_HOUR,
                Utility.STOCK_MARKET_UPDATE_MINUTE,
                0,
                0);

        // Check if the time is in the past so it doesn't trigger immediately.
        // If it is in the past, schedule the alarm for the next day.
        if (stockMarketClose.getTimeInMillis() <= System.currentTimeMillis()) {
            stockMarketClose.add(Calendar.DAY_OF_MONTH, 1);
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getRefreshPendingIntent(context)); //cancel previous alarms
        alarmManager.setInexactRepeating(AlarmManager.RTC,
                stockMarketClose.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                getRefreshPendingIntent(context));
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

            // Create an Intent to launch MainActivity from Logo
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingLogoIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.logo, pendingLogoIntent);

            // Create an Intent to refresh list
            PendingIntent pendingRefreshIntent = getRefreshPendingIntent(context);
            views.setOnClickPendingIntent(R.id.button_refresh, pendingRefreshIntent);

            // Set up the collection pending intent template
            boolean useDetailActivity = context.getResources()
                    .getBoolean(R.bool.is_phone);
            Intent clickIntentTemplate = useDetailActivity
                    ? new Intent(context, DetailActivity.class)
                    : new Intent(context, MainActivity.class);

            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntentTemplate);

            // Set up the collection adapter
            Intent remoteAdapterIntent = new Intent(context, StockWidgetRemoteViewsService.class);
            views.setRemoteAdapter(R.id.widget_list, remoteAdapterIntent);
            views.setEmptyView(R.id.widget_list, R.id.widget_empty);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        // Refresh the widget if possible
        onReceive(context, new Intent(Constants.ACTION_DATA_REFRESH));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // Log.d(TAG, "onReceive");
        String action = intent.getAction();
        if (action.equals(Constants.ACTION_DATA_UPDATED) || action.equals(Constants.ACTION_DATA_UPDATE_ERROR)
                || action.equals(Constants.ACTION_DATA_REFRESH)) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));

            if (appWidgetIds.length > 0) {
                switch (action) {
                    case Constants.ACTION_DATA_REFRESH:
                        if (!MyApplication.getInstance().isRefreshing()
                                && Utility.canUpdateList(context.getContentResolver())) {
                            //  Log.d(TAG, "ACTION_DATA_REFRESH");
                            MyApplication.getInstance().setRefreshing(true);
                            views.setViewVisibility(R.id.progress_wheel, View.VISIBLE);

                            Intent serviceIntent = new Intent(context, MainService.class);
                            serviceIntent.setAction(Constants.ACTION_WIDGET_REFRESH);
                            context.startService(serviceIntent);
                        }
                        break;

                    case Constants.ACTION_DATA_UPDATED:
                        // Log.d(TAG, "ACTION_DATA_UPDATED");
                        views.setViewVisibility(R.id.progress_wheel, View.INVISIBLE);
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
                        break;

                    case Constants.ACTION_DATA_UPDATE_ERROR:
                        // Log.d(TAG, "ACTION_DATA_UPDATE_ERROR");
                        views.setViewVisibility(R.id.progress_wheel, View.INVISIBLE);
                        break;
                }

                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views);
            }
        }
    }

    private PendingIntent getRefreshPendingIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(Constants.ACTION_DATA_REFRESH), 0);
    }

    @Override
    public void onDisabled(Context context) {
        // Cancel AlarmManager;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getRefreshPendingIntent(context));
    }
}
