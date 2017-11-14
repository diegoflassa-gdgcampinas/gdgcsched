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
package org.gdg_campinas.treffen.model;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.os.CancellationSignal;

import org.gdg_campinas.treffen.provider.ScheduleContract;
import org.gdg_campinas.treffen.util.CursorModelLoader;


public class TagMetadataCursorTransform implements CursorModelLoader.CursorTransform<TagMetadata> {

    @Override
    public Cursor performQuery(@NonNull CursorModelLoader<TagMetadata> loader,
            @NonNull CancellationSignal cancellationSignal) {
        return ContentResolverCompat.query(loader.getContext().getContentResolver(),
                ScheduleContract.Tags.CONTENT_URI, TagMetadata.TAGS_PROJECTION, null, null, null,
                cancellationSignal);
    }

    @Override
    public TagMetadata cursorToModel(@NonNull CursorModelLoader<TagMetadata> loader,
            @NonNull Cursor cursor) {
        return new TagMetadata(cursor);
    }

    @Override
    public Uri getObserverUri(@NonNull CursorModelLoader<TagMetadata> loader) {
        return ScheduleContract.Tags.CONTENT_URI;
    }
}
