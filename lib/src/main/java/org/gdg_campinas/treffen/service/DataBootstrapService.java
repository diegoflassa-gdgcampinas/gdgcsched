/*
 * Copyright 2015 Google Inc. All rights reserved.
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
package org.gdg_campinas.treffen.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.gdg_campinas.treffen.io.JSONHandler;
import org.gdg_campinas.treffen.lib.BuildConfig;
import org.gdg_campinas.treffen.provider.ScheduleContract;
import org.gdg_campinas.treffen.settings.SettingsUtils;
import org.gdg_campinas.treffen.sync.ConferenceDataHandler;
import org.gdg_campinas.treffen.sync.SyncHelper;
import org.gdg_campinas.treffen.util.LogUtils;

import java.io.IOException;

/**
 * An {@code IntentService} that performs the one-time data bootstrap. It takes the prepackaged
 * conference data from the R.raw.bootstrap_data resource, and populates the database. This data
 * contains the sessions, speakers, etc.
 */
public class DataBootstrapService extends IntentService {

    private static final String TAG = LogUtils.makeLogTag(DataBootstrapService.class);

    /**
     * Start the {@link DataBootstrapService} if the bootstrap is either not done or complete yet.
     *
     * @param context The context for starting the {@link IntentService} as well as checking if the
     *                shared preference to mark the process as done is set.
     */
    public static void startDataBootstrapIfNecessary(Context context) {
        if (!SettingsUtils.isDataBootstrapDone(context)) {
            LogUtils.LOGW(TAG, "One-time data bootstrap not done yet. Doing now.");
            context.startService(new Intent(context, DataBootstrapService.class));
        }
    }

    /**
     * Creates a DataBootstrapService.
     */
    public DataBootstrapService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context appContext = getApplicationContext();

        if (SettingsUtils.isDataBootstrapDone(appContext)) {
            LogUtils.LOGD(TAG, "Data bootstrap already done.");
            return;
        }
        try {
            LogUtils.LOGD(TAG, "Starting data bootstrap process.");
            // Load data from bootstrap raw resource.
            String bootstrapJson = JSONHandler
                    .parseResource(appContext, org.gdg_campinas.treffen.lib.R.raw.bootstrap_data);

            // Apply the data we read to the database with the help of the ConferenceDataHandler.
            ConferenceDataHandler dataHandler = new ConferenceDataHandler(appContext);
            dataHandler.applyConferenceData(new String[]{bootstrapJson},
                    BuildConfig.BOOTSTRAP_DATA_TIMESTAMP, false);

            SyncHelper.performPostSyncChores(appContext);

            LogUtils.LOGI(TAG, "End of bootstrap -- successful. Marking bootstrap as done.");
            SettingsUtils.markSyncSucceededNow(appContext);
            SettingsUtils.markDataBootstrapDone(appContext);

            getContentResolver().notifyChange(Uri.parse(ScheduleContract.CONTENT_AUTHORITY),
                    null, false);

        } catch (IOException ex) {
            // This is serious -- if this happens, the app won't work :-(
            // This is unlikely to happen in production, but IF it does, we apply
            // this workaround as a fallback: we pretend we managed to do the bootstrap
            // and hope that a remote sync will work.
            LogUtils.LOGE(TAG, "*** ERROR DURING BOOTSTRAP! Problem in bootstrap data?", ex);
            LogUtils.LOGE(TAG,
                    "Applying fallback -- marking boostrap as done; sync might fix problem.");
            SettingsUtils.markDataBootstrapDone(appContext);
        } finally {
            // Request a manual sync immediately after the bootstrapping process, in case we
            // have an active connection. Otherwise, the scheduled sync could take a while.
            SyncHelper.requestManualSync();
        }
    }
}
