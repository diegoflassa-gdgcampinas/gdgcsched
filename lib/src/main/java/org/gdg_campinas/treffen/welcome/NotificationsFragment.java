/*
 * Copyright (c) 2017 Google Inc.
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

package org.gdg_campinas.treffen.welcome;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.gdg_campinas.treffen.lib.R;
import org.gdg_campinas.treffen.settings.SettingsUtils;
import org.gdg_campinas.treffen.util.WelcomeUtils;

import org.gdg_campinas.treffen.util.LogUtils;

import static org.gdg_campinas.treffen.util.LogUtils.LOGI;
import static org.gdg_campinas.treffen.util.LogUtils.makeLogTag;

/**
 * Fragment that allows the user to opt into receiving notifications.
 */
public class NotificationsFragment extends WelcomeFragment {
    private static final String TAG = LogUtils.makeLogTag(NotificationsFragment.class);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.welcome_notifications_fragment, container, false);
    }

    @Override
    public boolean shouldDisplay(Context context) {
        /* Display only if the user hasn't opted into notifications and hasn't explicitly declined
        notifications during onboarding. */
        return !WelcomeUtils.hasUserDeclinedNotificationsDuringOnboarding(context) &&
                !SettingsUtils.shouldShowNotifications(context);
    }

    @Override
    protected View.OnClickListener getPrimaryButtonListener() {
        return new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(View v) {
                SettingsUtils.setShowNotifications(mActivity, true);
                LogUtils.LOGI(TAG, "User opted in to receive notifications");
                ((WelcomeActivity) mActivity).doNext();
            }
        };
    }

    @Override
    protected View.OnClickListener getSecondaryButtonListener() {
        return new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(final View view) {
                WelcomeUtils.markUserDeclinedNotificationsDuringOnboarding(mActivity);
                LogUtils.LOGI(TAG, "User opted out of receiving notifications");
                ((WelcomeActivity) mActivity).doNext();
            }
        };
    }

    @Override
    protected int getHeaderColorRes() {
        return R.color.sunflower_yellow;
    }

    @Override
    protected int getLogoDrawableRes() {
        return R.drawable.io_logo_onboarding;
    }

    @Override
    protected String getPrimaryButtonText() {
        return getResourceString(R.string.yes);
    }

    @Override
    protected String getSecondaryButtonText() {
        return getResourceString(R.string.no);
    }
}
