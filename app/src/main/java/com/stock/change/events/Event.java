package com.stock.change.events;


public class Event {
    private String mSessionId;

    public Event(String  sessionId){
        mSessionId = sessionId;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public void setSessionId(String sessionId) {
        mSessionId = sessionId;
    }
}
