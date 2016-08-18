package com.stock.change.custom;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;

import org.greenrobot.eventbus.EventBus;

import java.util.UUID;

/**
 * Custom Application class holds global state of the app.
 */
public class MyApplication extends Application {
    private static MyApplication sMyApplication;

    // Even though we are using TagManager to send Analytics Hits, we still need Tracker
    // to monitor uncaught exceptions
   // private Tracker mTracker;
    private TagManager mTagManager;
    private ContainerHolder mContainerHolder;

    private static String mSessionId = "";

    /**
     * This is true if the list is updating to the latest values.
     * (Swipe to Refresh/ Refresh Menu Btn/ Widget Refresh)
     */
    private static volatile boolean mRefreshing = false;


    @Override
    public void onCreate() {
        super.onCreate();
        sMyApplication = this;
        // Optimization to speed up event bus
        // http://greenrobot.org/eventbus/documentation/subscriber-index/
        EventBus.builder().addIndex(new MyEventBusIndex()).installDefaultEventBus();
        //ButterKnife.setDebug(true);

        AnalyticsTrackers.initialize(this);
        AnalyticsTrackers.getInstance().get(AnalyticsTrackers.Target.APP);
    }

    public static synchronized MyApplication getInstance() {
        return sMyApplication;
    }

    public synchronized Tracker getGoogleAnalyticsTracker() {
        AnalyticsTrackers analyticsTrackers = AnalyticsTrackers.getInstance();
        return analyticsTrackers.get(AnalyticsTrackers.Target.APP);
    }

    /***
     * Tracking screen view (Activity or Fragment)
     *
     * @param screenName screen name to be displayed on GA dashboard
     */
    public void trackScreenView(String screenName) {
        Tracker t = getGoogleAnalyticsTracker();
        // Set screen name.
        t.setScreenName(screenName);
        // Send a screen view.
        t.send(new HitBuilders.ScreenViewBuilder().build());
        GoogleAnalytics.getInstance(this).dispatchLocalHits();
    }


    /***
     * Tracking exception
     *
     * @param e exception to be tracked
     */
    public void trackException(Exception e) {
        if (e != null) {
            Tracker t = getGoogleAnalyticsTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription(
                            new StandardExceptionParser(this, null)
                                    .getDescription(Thread.currentThread().getName(), e))
                    .setFatal(false)
                    .build()
            );
        }
    }


    /***
     * Tracking event
     *
     * @param category event category
     * @param action   action of the event
     * @param label    label
     */
    public void trackEvent(String category, String action, String label) {
        Tracker t = getGoogleAnalyticsTracker();
        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder().setCategory(category).setAction(action).setLabel(label).build());
    }


    public static void startNewSession() {
        mSessionId = UUID.randomUUID().toString();
    }

    public static boolean validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        return mSessionId.equals(sessionId);
    }

   /* *//**
     * Initializes the Analytics Tracker
     *//*
    public synchronized void initAnalyticsTracking() {
        if (mTracker == null) {
            // get google analytics instance
            GoogleAnalytics ga = GoogleAnalytics.getInstance(this);
            // To enable debug logging use:
            // adb shell setprop log.tag.GAv4 DEBUG
            // adb logcat -s GAv4
            // create a new tracker
            mTracker = ga.newTracker(R.xml.analytics_tracker);
            //enable automatic reports
            ga.enableAutoActivityReports(this);

        }
    }
*/
   /* public Tracker getAnalyticsTracker() {
        return mTracker;
    }*/

    public synchronized TagManager getTagManager() {
        if (mTagManager == null) {
            mTagManager = TagManager.getInstance(this);
        }
        return mTagManager;
    }

    public ContainerHolder getContainerHolder() {
        return mContainerHolder;
    }

    public void setContainerHolder(ContainerHolder containerHolder) {
        mContainerHolder = containerHolder;
    }


    public String getSessionId() {
        return mSessionId;
    }

    public boolean isRefreshing() {
        return mRefreshing;
    }

    public void setRefreshing(boolean refreshing) {
        mRefreshing = refreshing;
    }
}