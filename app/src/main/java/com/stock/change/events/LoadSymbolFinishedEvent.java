package com.stock.change.events;

import com.stock.change.pojo.StockChange;


public class LoadSymbolFinishedEvent  extends Event {
    private StockChange mStock;
    private boolean mSuccessful;

    public LoadSymbolFinishedEvent(String sessionId, StockChange stock, boolean successful){
        super(sessionId);
        mStock = stock;
        mSuccessful = successful;
    }

    public StockChange getStock() {
        return mStock;
    }

    public void setStock(StockChange stock) {
        mStock = stock;
    }

    public boolean isSuccessful() {
        return mSuccessful;
    }

    public void setSuccessful(boolean successful) {
        mSuccessful = successful;
    }
}
