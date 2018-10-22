package com.zippy.zippykiosk;

/**
 * Created by KB on 4/04/15.
 * Copyright 2015 Zippy.com.au.
 */
public class RewardItem {
    final int id;
    final int points;
    final String title;

    RewardItem(int id, String title, int points) {
        this.id = id;
        this.title = title;
        this.points = points;
    }


}

