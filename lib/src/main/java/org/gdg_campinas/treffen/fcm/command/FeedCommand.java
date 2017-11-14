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
package org.gdg_campinas.treffen.fcm.command;

import android.content.Context;

import org.gdg_campinas.treffen.fcm.FcmCommand;
import org.gdg_campinas.treffen.util.LogUtils;
import org.gdg_campinas.treffen.feed.FeedState;

import static org.gdg_campinas.treffen.util.LogUtils.LOGI;
import static org.gdg_campinas.treffen.util.LogUtils.makeLogTag;

/**
 * Received when remote Feed is updated.
 */
public class FeedCommand extends FcmCommand {
    private static final String TAG = LogUtils.makeLogTag(FeedCommand.class);

    @Override
    public void execute(Context context, String type, String extraData) {
        LogUtils.LOGI(TAG, "New Feed message received");
        FeedState.getInstance().updateNewFeedItem(context, true);
    }
}
