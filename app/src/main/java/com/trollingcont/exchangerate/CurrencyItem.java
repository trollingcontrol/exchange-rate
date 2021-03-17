package com.trollingcont.exchangerate;

public class CurrencyItem {
    private String displayedItemText;

    public CurrencyItem(String currencyInfo) {
        displayedItemText = currencyInfo;
    }

    public String getText() {
        return displayedItemText;
    }
}