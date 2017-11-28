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
package org.gdg_campinas.treffen.fcm.command;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.gdg_campinas.treffen.fcm.FcmCommand;
import org.gdg_campinas.treffen.myio.MyIOActivity;
import org.gdg_campinas.treffen.util.LogUtils;

public class AnnouncementCommand extends FcmCommand {
    private static final String TAG = LogUtils.makeLogTag("AnnouncementCommand");

    @Override
    public void execute(Context context, String type, String extraData) {
        LogUtils.LOGI(TAG, "Received FCM message: " + type);
        displayNotification(context, extraData);
    }

    private void displayNotification(Context context, String message) {
        LogUtils.LOGI(TAG, "Displaying notification: " + message);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(0, new NotificationCompat.Builder(context)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(org.gdg_campinas.treffen.lib.R.drawable.ic_stat_notification)
                        .setTicker(message)
                        .setContentTitle(context.getString(org.gdg_campinas.treffen.lib.R.string.app_name))
                        .setContentText(message)
                        //.setColor(context.getResources().getColor(R.color.theme_primary))
                        // Note: setColor() is available in the support lib v21+.
                        // We commented it out because we want the source to compile
                        // against support lib v20. If you are using support lib
                        // v21 or above on Android L, uncomment this line.
                        .setContentIntent(
                                PendingIntent.getActivity(context, 0,
                                        new Intent(context, MyIOActivity.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                        Intent.FLAG_ACTIVITY_SINGLE_TOP),
                                        0))
                        .setAutoCancel(true)
                        .build());
    }

}
