/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gdg_campinas.treffen.myio;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.os.CancellationSignal;

import org.gdg_campinas.treffen.model.ScheduleItem;
import org.gdg_campinas.treffen.model.ScheduleItemHelper;
import org.gdg_campinas.treffen.util.CursorModelLoader;
import org.gdg_campinas.treffen.provider.ScheduleContract.Sessions;

import java.util.List;


public class MyIOScheduleCursorTransform implements CursorModelLoader.CursorTransform<List<ScheduleItem>> {

    @Override
    public Cursor performQuery(@NonNull CursorModelLoader<List<ScheduleItem>> loader,
            @NonNull CancellationSignal cancellationSignal) {
        return ContentResolverCompat.query(loader.getContext().getContentResolver(),
                Sessions.CONTENT_MY_SCHEDULE_URI, ScheduleItemHelper.REQUIRED_SESSION_COLUMNS,
                null, null, Sessions.SORT_BY_TIME, cancellationSignal);
    }

    @Override
    public List<ScheduleItem> cursorToModel(@NonNull CursorModelLoader<List<ScheduleItem>> loader,
            @NonNull Cursor cursor) {
        return ScheduleItemHelper.cursorToItems(cursor, loader.getContext());
    }

    @Override
    public Uri getObserverUri(@NonNull CursorModelLoader<List<ScheduleItem>> loader) {
        return Sessions.CONTENT_MY_SCHEDULE_URI;
    }
}
