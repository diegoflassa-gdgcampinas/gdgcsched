/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gdg_campinas.treffen.model;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.gdg_campinas.treffen.lib.BuildConfig;
import org.gdg_campinas.treffen.provider.ScheduleContract;
import org.gdg_campinas.treffen.schedule.ScheduleModel;
import org.gdg_campinas.treffen.schedule.TagFilterHolder;
import org.gdg_campinas.treffen.util.LogUtils;

import java.util.ArrayList;

public class ScheduleHelper {

    public static final int MODE_ALL_ITEMS = 0;
    public static final int MODE_STARRED_ITEMS = 1;

    private static final String TAG = LogUtils.makeLogTag(ScheduleHelper.class);

    private final Context mContext;
    private final int mMode;

    public ScheduleHelper(@NonNull Context context) {
        mContext = context;
        mMode = MODE_ALL_ITEMS;
    }

    public ScheduleHelper(@NonNull Context context, int mode) {
        mContext = context;
        mMode = mode;
    }

    public ArrayList<ScheduleItem> getScheduleData(final long start, final long end,
            @Nullable final TagFilterHolder filters) {
        // get sessions in my schedule and blocks, starting anytime in the conference day
        final ArrayList<ScheduleItem> items = new ArrayList<>();

        addSessions(start, end, items, filters);

        ArrayList<ScheduleItem> result = ScheduleItemHelper.processItems(items);
        if (BuildConfig.DEBUG || Log.isLoggable(TAG, Log.DEBUG)) {
            ScheduleItem previous = null;
            for (ScheduleItem item : result) {
                if ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) != 0) {
                    Log.d(TAG, "Schedule Item conflicts with previous. item="
                            + item + " previous=" + previous);
                }
                previous = item;
            }
        }
        return result;
    }

    public void getScheduleDataAsync(
            final @NonNull ScheduleModel.LoadScheduleDataListener callback,
            long start, long end, @Nullable final TagFilterHolder filters) {
        AsyncTask<Long, Void, ArrayList<ScheduleItem>> task
                = new AsyncTask<Long, Void, ArrayList<ScheduleItem>>() {
            @Override
            protected ArrayList<ScheduleItem> doInBackground(Long... params) {
                Long start = params[0];
                Long end = params[1];
                return getScheduleData(start, end, filters);
            }

            @Override
            protected void onPostExecute(ArrayList<ScheduleItem> scheduleItems) {
                callback.onDataLoaded(scheduleItems);
            }
        };
        // On honeycomb and above, AsyncTasks are by default executed one by one. We are using a
        // thread pool instead here, because we want this to be executed independently from other
        // AsyncTasks. See the URL below for detail.
        // http://developer.android.com/reference/android/os/AsyncTask.html#execute(Params...)
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, start, end);
    }

    protected void addSessions(final long start, final long end,
            @NonNull final ArrayList<ScheduleItem> items, final TagFilterHolder filters) {
        Cursor cursor = null;
        try {
            Uri uri = ScheduleContract.Sessions.CONTENT_URI;
            String selection = ScheduleContract.Sessions.STARTING_AT_TIME_INTERVAL_SELECTION;
            if (mMode == MODE_STARRED_ITEMS) {
                selection = DatabaseUtils.concatenateWhere(selection,
                        ScheduleContract.Sessions.IN_SCHEDULE_SELECTION);
            }
            if (filters != null) {
                uri = ScheduleContract.Sessions.buildCategoryTagFilterUri(uri, filters.getSelectedFilterIds(),
                        filters.getCategoryCount());
            }
            cursor = mContext.getContentResolver().query(
                    uri,
                    ScheduleItemHelper.REQUIRED_SESSION_COLUMNS,
                    selection,
                    new String[]{String.valueOf(start), String.valueOf(end)},
                    // order by session start
                    ScheduleContract.Sessions.SESSION_START);

            ScheduleItemHelper.cursorToItems(cursor, mContext, items);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
