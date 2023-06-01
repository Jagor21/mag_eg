package com.eternal_joy.magic_egypt;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;

public class MyLayoutManager extends LinearLayoutManager {

    private boolean isScrollEnabled = true;

    public MyLayoutManager(Context context) {
        super(context);
    }

    public void setScrollEnabled(boolean flag) {
        isScrollEnabled = flag;
    }

    @Override
    public boolean canScrollVertically() {
        return isScrollEnabled && super.canScrollVertically();
    }
}
