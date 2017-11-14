/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gdg_campinas.treffen.fcm;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import org.gdg_campinas.treffen.settings.ConfMessageCardUtils;
import org.gdg_campinas.treffen.util.AccountUtils;
import org.gdg_campinas.treffen.util.LogUtils;
import org.gdg_campinas.treffen.util.RegistrationUtils;

import static org.gdg_campinas.treffen.fcm.FcmUtilities.subscribeTopics;
import static org.gdg_campinas.treffen.util.LogUtils.LOGI;
import static org.gdg_campinas.treffen.util.LogUtils.LOGV;
import static org.gdg_campinas.treffen.util.LogUtils.makeLogTag;

/**
 * In the event that the current InstanceID token is invalidated, this service is triggered and
 * the token is refreshed. We then must update the server with the refreshed token.
 */
public class MyInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = LogUtils.makeLogTag("FCMIIDListenerService");

    @Override
    public void onTokenRefresh() {
        LogUtils.LOGV(TAG, "Set registered to false");
        ServerUtilities.setRegisteredOnServer(this, false, ServerUtilities.getDeviceId(this), null);

        // Get the correct FCM key for the user. FCM key is a somewhat non-standard
        // approach we use in this app. For more about this, check FCM.md.
        final String fcmKey = AccountUtils.hasActiveAccount(this) ?
                AccountUtils.getActiveAccountId(this) : null;

        // Unregister on server.
        ServerUtilities.unregister(this, ServerUtilities.getDeviceId(this));

        // Register for a new InstanceID token. This token is sent to the server to be paired with
        // the current user's FCM key.
        if(FirebaseInstanceId.getInstance().getToken() != null) {
            sendRegistrationToServer(FirebaseInstanceId.getInstance().getToken(), fcmKey);
            subscribeTopics(ConfMessageCardUtils.isConfMessageCardsEnabled(this),
                    RegistrationUtils.isRegisteredAttendee(this) ==
                            RegistrationUtils.REGSTATUS_REGISTERED);
        }
    }

    /**
     * Send the refreshed InstanceID token to the server to be paired with the user identifying
     * fcmKey.
     *
     * @param token  InstanceID token that FCM uses to send messages to this application instance.
     * @param fcmKey String used to pair a user with an InstanceID token.
     */
    private void sendRegistrationToServer(String token, String fcmKey) {
        if (!ServerUtilities.isRegisteredOnServer(this, fcmKey)) {
            LogUtils.LOGI(TAG, "Registering on the FCM server with FCM key: " + fcmKey);
            boolean registered = ServerUtilities.register(this, token, fcmKey);

            if (!registered) {
                // At this point all attempts to register with the app
                // server failed, the app will try to register again when
                // it is restarted.
                LogUtils.LOGI(TAG, "FCM registration failed.");
            } else {
                LogUtils.LOGI(TAG, "FCM registration successful.");
                ServerUtilities.setRegisteredOnServer(this, true, token, fcmKey);
            }
        } else {
            LogUtils.LOGI(TAG, "Already registered on the FCM server with FCM key " + fcmKey);
        }
    }
}
