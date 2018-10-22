package com.zippy.zippykiosk;

import android.support.annotation.Nullable;

import com.zippy.zippykiosk.rest.Business;
import com.zippy.zippykiosk.rest.Reward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by KB on 6/04/15.
 * Copyright 2015 Zippy.com.au.
 */
class DB {
    public static long timestamp;

    static List<RewardItem> getRewardsList(@Nullable Business business) {
        return DB.getRewardsList(business==null ? null : business.rewards);
    }
    static List<RewardItem> getRewardsList(@Nullable Reward[] rewards) {
        List<RewardItem> list = new ArrayList<>();
        if(rewards!=null) {
            for (Reward reward : rewards) {
                if ((reward.status & 1) == 1) { // Reward status is enabled
                    list.add(new RewardItem(reward.id, reward.name, reward.points));
                }
            }

            Collections.sort(list, new Comparator<RewardItem>() {

                @Override
                public int compare(RewardItem item1, RewardItem item2) {
                    if (item1.points > item2.points) {
                        return 1;
                    } else if (item1.points < item2.points) {
                        return -1;
                    } else {
                        return 0;
                    }
                }

            });
        }
        return list;
    }
}
