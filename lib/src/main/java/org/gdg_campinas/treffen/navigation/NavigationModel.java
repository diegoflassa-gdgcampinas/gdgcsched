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

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.gdg_campinas.treffen.archframework.UserActionEnum;
import org.gdg_campinas.treffen.feed.FeedActivity;
import org.gdg_campinas.treffen.info.InfoActivity;
import org.gdg_campinas.treffen.map.MapActivity;
import org.gdg_campinas.treffen.myio.MyIOActivity;
import org.gdg_campinas.treffen.schedule.ScheduleActivity;
import org.gdg_campinas.treffen.archframework.Model;
import org.gdg_campinas.treffen.archframework.QueryEnum;
import org.gdg_campinas.treffen.navigation.NavigationModel.NavigationQueryEnum;
import org.gdg_campinas.treffen.navigation.NavigationModel.NavigationUserActionEnum;

/**
 * Determines which items to show in the {@link AppNavigationView}.
 */
public class NavigationModel implements Model<NavigationQueryEnum, NavigationUserActionEnum> {

    private NavigationItemEnum[] mItems;

    public NavigationItemEnum[] getItems() {
        return mItems;
    }

    @Override
    public NavigationQueryEnum[] getQueries() {
        return NavigationQueryEnum.values();
    }

    @Override
    public NavigationUserActionEnum[] getUserActions() {
        return NavigationUserActionEnum.values();
    }

    @Override
    public void deliverUserAction(final NavigationUserActionEnum action,
            @Nullable final Bundle args,
            final UserActionCallback<NavigationUserActionEnum> callback) {
        switch (action) {
            case RELOAD_ITEMS:
                mItems = null;
                populateNavigationItems();
                callback.onModelUpdated(this, action);
                break;
        }
    }

    @Override
    public void requestData(final NavigationQueryEnum query,
            final DataQueryCallback<NavigationQueryEnum> callback) {
        switch (query) {
            case LOAD_ITEMS:
                if (mItems != null) {
                    callback.onModelUpdated(this, query);
                } else {
                    populateNavigationItems();
                    callback.onModelUpdated(this, query);
                }
                break;
        }
    }

    private void populateNavigationItems() {
        NavigationItemEnum[] items = NavigationConfig.ITEMS;
        mItems = NavigationConfig.filterOutItemsDisabledInBuildConfig(items);
    }

    @Override
    public void cleanUp() {
        // no-op
    }

    /**
     * List of all possible navigation items.
     */
    public enum NavigationItemEnum {
        MY_IO(org.gdg_campinas.treffen.lib.R.id.myio_nav_item, org.gdg_campinas.treffen.lib.R.string.navdrawer_item_my_io, org.gdg_campinas.treffen.lib.R.drawable.ic_nav_myio,
                MyIOActivity.class, true),
        MY_SCHEDULE(org.gdg_campinas.treffen.lib.R.id.myschedule_nav_item, org.gdg_campinas.treffen.lib.R.string.navdrawer_item_my_schedule,
                org.gdg_campinas.treffen.lib.R.drawable.ic_nav_schedule, ScheduleActivity.class, true),
        FEED(org.gdg_campinas.treffen.lib.R.id.feed_nav_item, org.gdg_campinas.treffen.lib.R.string.navdrawer_item_feed,
                org.gdg_campinas.treffen.lib.R.drawable.ic_nav_feed, FeedActivity.class, true),
        MAP(org.gdg_campinas.treffen.lib.R.id.map_nav_item, org.gdg_campinas.treffen.lib.R.string.navdrawer_item_map, org.gdg_campinas.treffen.lib.R.drawable.ic_nav_map,
                MapActivity.class, true),
        INFO(org.gdg_campinas.treffen.lib.R.id.info_nav_item, org.gdg_campinas.treffen.lib.R.string.navdrawer_item_info,
                org.gdg_campinas.treffen.lib.R.drawable.ic_nav_info, InfoActivity.class, true),
        INVALID(12, 0, 0, null);
        private int id;

        private int titleResource;

        private int iconResource;

        private Class classToLaunch;

        private boolean finishCurrentActivity;

        NavigationItemEnum(int id, int titleResource, int iconResource, Class classToLaunch) {
            this(id, titleResource, iconResource, classToLaunch, false);
        }

        NavigationItemEnum(int id, int titleResource, int iconResource, Class classToLaunch,
                boolean finishCurrentActivity) {
            this.id = id;
            this.titleResource = titleResource;
            this.iconResource = iconResource;
            this.classToLaunch = classToLaunch;
            this.finishCurrentActivity = finishCurrentActivity;
        }

        public static NavigationItemEnum getById(int id) {
            for (NavigationItemEnum value : NavigationItemEnum.values()) {
                if (value.getId() == id) {
                    return value;
                }
            }
            return INVALID;
        }

        public int getId() {
            return id;
        }

        public int getTitleResource() {
            return titleResource;
        }

        public int getIconResource() {
            return iconResource;
        }

        public Class getClassToLaunch() {
            return classToLaunch;
        }

        public boolean finishCurrentActivity() {
            return finishCurrentActivity;
        }

    }

    public enum NavigationQueryEnum implements QueryEnum {
        LOAD_ITEMS(0);

        private int id;

        NavigationQueryEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String[] getProjection() {
            return new String[0];
        }
    }

    public enum NavigationUserActionEnum implements UserActionEnum {
        RELOAD_ITEMS(0);

        private int id;

        NavigationUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }
    }
}
