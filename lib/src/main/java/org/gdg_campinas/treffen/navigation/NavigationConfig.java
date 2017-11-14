/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.gdg_campinas.treffen.navigation;

import org.gdg_campinas.treffen.lib.BuildConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration file for items to show in the {@link AppNavigationView}. This is used by the {@link
 * NavigationModel}.
 */
public class NavigationConfig {

    public final static NavigationModel.NavigationItemEnum[] ITEMS = new NavigationModel.NavigationItemEnum[]{
            NavigationModel.NavigationItemEnum.MY_SCHEDULE,
            NavigationModel.NavigationItemEnum.MY_IO,
            NavigationModel.NavigationItemEnum.FEED,
            NavigationModel.NavigationItemEnum.MAP,
            NavigationModel.NavigationItemEnum.INFO,
    };

    private static NavigationModel.NavigationItemEnum[] concatenateItems(NavigationModel.NavigationItemEnum[] first,
                                                                         NavigationModel.NavigationItemEnum[] second) {
        NavigationModel.NavigationItemEnum[] items = new NavigationModel.NavigationItemEnum[first.length + second.length];
        for (int i = 0; i < first.length; i++) {
            items[i] = first[i];
        }
        for (int i = 0; i < second.length; i++) {
            items[first.length + i] = second[i];
        }
        return items;
    }

    public static NavigationModel.NavigationItemEnum[] appendItem(NavigationModel.NavigationItemEnum[] first,
                                                                  NavigationModel.NavigationItemEnum second) {
        return concatenateItems(first, new NavigationModel.NavigationItemEnum[]{second});
    }

    public static NavigationModel.NavigationItemEnum[] filterOutItemsDisabledInBuildConfig(
            NavigationModel.NavigationItemEnum[] items) {
        List<NavigationModel.NavigationItemEnum> enabledItems = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            boolean includeItem = true;
            switch (items[i]) {
                case MY_SCHEDULE:
                    includeItem = BuildConfig.ENABLE_MYSCHEDULE_IN_NAVIGATION;
                    break;
                case MY_IO:
                    includeItem = BuildConfig.ENABLE_MYIO_IN_NAVIGATION;
                    break;
                case FEED:
                    includeItem = BuildConfig.ENABLE_FEED_IN_NAVIGATION;
                    break;
                case MAP:
                    includeItem = BuildConfig.ENABLE_MAP_IN_NAVIGATION;
                    break;
                case INFO:
                    includeItem = BuildConfig.ENABLE_INFO_IN_NAVIGATION;
                    break;
            }

            if (includeItem) {
                enabledItems.add(items[i]);
            }
        }
        return enabledItems.toArray(new NavigationModel.NavigationItemEnum[enabledItems.size()]);
    }

}
