/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package org.gdg_campinas.treffen.signin;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import org.gdg_campinas.treffen.sync.SyncHelper;
import org.gdg_campinas.treffen.provider.ScheduleContract;
import org.gdg_campinas.treffen.provider.ScheduleContractHelper;
import org.gdg_campinas.treffen.util.LogUtils;

import java.util.ArrayList;

import static org.gdg_campinas.treffen.util.LogUtils.LOGE;
import static org.gdg_campinas.treffen.util.LogUtils.LOGV;
import static org.gdg_campinas.treffen.util.LogUtils.makeLogTag;

/**
 * An {@code IntentService} with the sole purpose of upgrading any
 * user data associated with a non-logged user to the signed in user.
 */
public class PostSignInUpgradeService extends IntentService {

    private static final String TAG = LogUtils.makeLogTag(PostSignInUpgradeService.class);
    public static final String KEY_ACCOUNT_NAME = "KEY_ACCOUNT_NAME";

    public PostSignInUpgradeService() {
        super("PostSignInUpgradeService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        String accountName = intent.getStringExtra(KEY_ACCOUNT_NAME);
        if (accountName == null) {
            return;
        }

        // TODO: It would be better if the default value for the account in
        // the non-signed in case was not "null". When that is modified update this.
        String previousAccountName = null;

        // The task is to upgrade all user data that was associated with a non-logged in user
        // and update it to the signed in user.
        ArrayList<ContentProviderOperation> ops = new ArrayList<>(3);
        //noinspection ConstantConditions
        ops.add(ContentProviderOperation
                .newUpdate(ScheduleContractHelper.addOverrideAccountUpdateAllowed(
                        ScheduleContract.MySchedule.buildMyScheduleUri(previousAccountName)))
                .withValue(ScheduleContract.MySchedule.MY_SCHEDULE_ACCOUNT_NAME, accountName)
                .build());

        //noinspection ConstantConditions
        ops.add(ContentProviderOperation
                .newUpdate(ScheduleContractHelper.addOverrideAccountUpdateAllowed(
                        ScheduleContract.MyFeedbackSubmitted.buildMyFeedbackSubmittedUri(previousAccountName)))
                .withValue(ScheduleContract.MyFeedbackSubmitted.MY_FEEDBACK_SUBMITTED_ACCOUNT_NAME, accountName)
                .build());

        // Delete any reservations (should be none)
        //noinspection ConstantConditions
        ops.add(ContentProviderOperation
                .newDelete(ScheduleContract.MyReservations.buildMyReservationUri(previousAccountName))
                .withSelection(ScheduleContract.MyReservations.MY_RESERVATION_ACCOUNT_NAME, null)
                .build());

        try {
            ContentProviderResult[] results =
                    getContentResolver().applyBatch(ScheduleContract.CONTENT_AUTHORITY, ops);
            if (LogUtils.LOGGING_ENABLED) {
                for (ContentProviderResult res : results) {
                    LogUtils.LOGV(TAG, "Result of update: uri: " + res.uri + " count: " + res.count);
                }
            }
        } catch (RemoteException | OperationApplicationException e) {
            LogUtils.LOGE(TAG, "Unexpected exception upgrading the user data to signed in user", e);
        } finally {
            // Note: Once we are done with the upgrade we trigger a manual sync for user data
            SyncHelper.requestManualSync(true);
        }
    }

    public static void upgradeToSignedInUser(Context context, String activeAccount) {
        Intent intent = new Intent(context, PostSignInUpgradeService.class);
        intent.putExtra(KEY_ACCOUNT_NAME, activeAccount);
        context.startService(intent);
    }
}
