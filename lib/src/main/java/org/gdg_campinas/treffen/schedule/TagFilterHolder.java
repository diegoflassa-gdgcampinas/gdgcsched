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

package org.gdg_campinas.treffen.schedule;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import org.gdg_campinas.treffen.Config.Tags;
import org.gdg_campinas.treffen.lib.R;

import org.gdg_campinas.treffen.model.TagMetadata;
import org.gdg_campinas.treffen.util.LogUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.gdg_campinas.treffen.util.LogUtils.makeLogTag;

/**
 * Class responsible for storing, managing and retrieving Tag filters used in the filter drawer.
 */
public class TagFilterHolder implements Parcelable {

    private static final String TAG = LogUtils.makeLogTag(TagFilterHolder.class);

    private final Set<TagMetadata.Tag> mSelectedTypes;
    private final Set<TagMetadata.Tag> mSelectedTopics;

    public TagFilterHolder() {
        mSelectedTypes = new HashSet<>();
        mSelectedTopics = new HashSet<>();
    }

    private TagFilterHolder(Parcel in) {
        this();
        readFromParcel(in);
    }

    private static boolean isCategoryValid(String category) {
        return Tags.CATEGORY_TYPE.equals(category) || Tags.CATEGORY_TRACK.equals(category);
    }

    /**
     * @param tag The tag to check in the filter
     * @return boolean Return a boolean indicating that the tagId is present.
     */
    public boolean contains(TagMetadata.Tag tag) {
        return mSelectedTypes.contains(tag) || mSelectedTopics.contains(tag);
    }

    /**
     * Add a tag to the set of filters
     *
     * @param tag The tag to be included in the filter.
     * @return True if the set of filters was modified by this call.
     */
    public boolean add(TagMetadata.Tag tag) {
        final String category = tag.getCategory();
        if (isCategoryValid(tag.getCategory())) {
            if (Tags.CATEGORY_TYPE.equals(category)) {
                return mSelectedTypes.add(tag);
            }
            if (Tags.CATEGORY_TRACK.equals(category)) {
                return mSelectedTopics.add(tag);
            }
        }
        return false;
    }

    /**
     * @param tag The tag to be removed from the filter set.
     * @return True if the set of filters was modified by this call.
     */
    public boolean remove(TagMetadata.Tag tag) {
        return mSelectedTypes.remove(tag) || mSelectedTopics.remove(tag);
    }

    public void clear() {
        mSelectedTypes.clear();
        mSelectedTopics.clear();
    }

    /**
     * @return String[] containing all the tag IDs to filter on.
     */
    public String getSelectedFilterIds()[] {
        final int size = mSelectedTypes.size() + mSelectedTopics.size();
        String[] a = new String[size];
        int i = 0;
        for (TagMetadata.Tag tag : mSelectedTypes) {
            a[i++] = tag.getId();
        }
        for (TagMetadata.Tag tag : mSelectedTopics) {
            a[i++] = tag.getId();
        }
        return a;
    }

    /**
     * Returns the number of tag categories for the filter query against the content provider.
     */
    public int getCategoryCount() {
        return (mSelectedTypes.isEmpty() ? 0 : 1) + (mSelectedTopics.isEmpty() ? 0 : 1);
    }

    /**
     * @return true if any filters are active, including non-topic filters (e.g. live streamed)
     */
    public boolean hasAnyFilters() {
        return !mSelectedTypes.isEmpty() || !mSelectedTopics.isEmpty();
    }

    public CharSequence describeFilters(Resources res, Theme theme) {
        if (!hasAnyFilters()) {
            return null;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(res.getString(R.string.active_filters_description));
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // TODO this could probably be cleaner
        int filterNamesStart = builder.length();
        boolean needComma = false;
        for (TagMetadata.Tag tag : mSelectedTypes) {
            if (needComma) {
                builder.append(", ");
            }
            builder.append(tag.getName());
            needComma = true;
        }
        for (TagMetadata.Tag tag : mSelectedTopics) {
            if (needComma) {
                builder.append(", ");
            }
            builder.append(tag.getName());
            needComma = true;
        }

        int color = ResourcesCompat.getColor(res, R.color.lightish_blue_a11y, theme);
        builder.setSpan(new ForegroundColorSpan(color), filterNamesStart, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    // -- Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        writeTagSetToParcel(mSelectedTypes, dest);
        writeTagSetToParcel(mSelectedTopics, dest);
    }

    private void writeTagSetToParcel(Set<TagMetadata.Tag> set, Parcel dest) {
        final int size = set.size();
        TagMetadata.Tag[] tags = set.toArray(new TagMetadata.Tag[size]);
        dest.writeInt(size);
        dest.writeParcelableArray(tags, 0);
    }

    private void readFromParcel(Parcel in) {
        readTagSetFromParcel(mSelectedTypes, in);
        readTagSetFromParcel(mSelectedTopics, in);
    }

    private void readTagSetFromParcel(Set<TagMetadata.Tag> set, Parcel in) {
        final int size = in.readInt();
        TagMetadata.Tag[] tags = new TagMetadata.Tag[size];
        in.readTypedArray(tags, TagMetadata.Tag.CREATOR);
        Collections.addAll(set, tags);
    }

    public static final Creator<TagFilterHolder> CREATOR = new Creator<TagFilterHolder>() {

        @Override
        public TagFilterHolder createFromParcel(Parcel in) {
            return new TagFilterHolder(in);
        }

        @Override
        public TagFilterHolder[] newArray(int size) {
            return new TagFilterHolder[size];
        }
    };
}
