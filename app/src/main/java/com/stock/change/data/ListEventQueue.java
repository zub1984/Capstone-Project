package com.stock.change.data;

import com.stock.change.events.AppRefreshFinishedEvent;
import com.stock.change.events.Event;
import com.stock.change.events.InitLoadFromDbFinishedEvent;
import com.stock.change.events.LoadMoreFinishedEvent;
import com.stock.change.events.LoadSymbolFinishedEvent;
import com.stock.change.events.MainProgressWheelHideEvent;
import com.stock.change.events.MainProgressWheelShowEvent;
import com.stock.change.events.WidgetRefreshDelegateEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This is a queue for our EventBus events. If an event is sent with no subscriber OR if queue not
 * empty, it will be put into the queue. This can occur during orientation change which can cause
 * a subscriber to unregister right as the event is posted. After orientation change it should
 * process events from this queue.
 */
public class ListEventQueue {
    private static final String TAG = ListEventQueue.class.getSimpleName();
    private static ListEventQueue sListEventQueue;
    private Queue<Event> mQueue;

    private ListEventQueue() {
        mQueue = new LinkedList<>();
    }

    public synchronized static ListEventQueue getInstance() {
        if (sListEventQueue == null) {
            sListEventQueue = new ListEventQueue();
        }
        return sListEventQueue;
    }

    /**
     * Posts an event to {@link EventBus}. If there is no subscribers for the event or if
     * there are already events on the event queue, the event will be added to the event queue.
     *
     * @param event The {@link Event} to be posted.
     */
    public void post(Event event) {
        EventBus eventBus = EventBus.getDefault();
        Class c = getEventType(event);

        if (eventBus.hasSubscriberForEvent(c) && mQueue.isEmpty()) {
            eventBus.postSticky(event);
        } else {
            mQueue.offer(event);
        }
    }

    /**
     * Post all events that are queue in the event queue.
     */
    public void postAllFromQueue() {
        EventBus eventBus = EventBus.getDefault();

        while (!mQueue.isEmpty()) {
            Class event = getEventType(mQueue.peek());

            if (eventBus.hasSubscriberForEvent(event)) {
                eventBus.postSticky(mQueue.poll());
            } else {
                break;
            }
        }
    }

    /**
     * Determines what class the event is.
     *
     * @param event The event to figure out the type for.
     * @return Class obj that represents the event class.
     */
    private Class getEventType(Event event) {
        // After every event remember to remove its sticky event or events will be performed twice
        if (event instanceof LoadMoreFinishedEvent) {
            return LoadMoreFinishedEvent.class;

        } else if (event instanceof LoadSymbolFinishedEvent) {
            return LoadSymbolFinishedEvent.class;

        } else if (event instanceof WidgetRefreshDelegateEvent) {
            return WidgetRefreshDelegateEvent.class;

        } else if (event instanceof AppRefreshFinishedEvent) {
            return AppRefreshFinishedEvent.class;

        } else if (event instanceof InitLoadFromDbFinishedEvent) {
            return InitLoadFromDbFinishedEvent.class;

        } else if (event instanceof MainProgressWheelShowEvent) {
            return AppRefreshFinishedEvent.class;

        } else if (event instanceof MainProgressWheelHideEvent) {
            return InitLoadFromDbFinishedEvent.class;
        }

        return null;
    }

    public Object peek() {
        return mQueue.peek();
    }

    public void clearQueue() {
        mQueue.clear();
    }

    public boolean isEmpty() {
        return mQueue.isEmpty();
    }
}


