package com.stock.change.events;

import com.stock.change.pojo.StockChange;

import java.util.List;


public class LoadMoreFinishedEvent extends Event {
    private List<StockChange> mStockList;
    private boolean mSuccessful;

    public LoadMoreFinishedEvent(String sessionId, List<StockChange> stockList, boolean successful){
        super(sessionId);
        mStockList = stockList;
        mSuccessful = successful;
    }

    public List<StockChange> getStockList() {
        return mStockList;
    }

    public void setStockList(List<StockChange> stockList) {
        mStockList = stockList;
    }

    public boolean isSuccessful() {
        return mSuccessful;
    }

    public void setSuccessful(boolean successful) {
        mSuccessful = successful;
    }
}
