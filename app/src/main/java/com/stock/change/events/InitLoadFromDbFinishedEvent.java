package com.stock.change.events;


public class InitLoadFromDbFinishedEvent extends Event {

    public InitLoadFromDbFinishedEvent(String sessionId) {
        super(sessionId);
    }
}
