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
package org.gdg_campinas.treffen.schedule;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import org.gdg_campinas.treffen.Config;
import org.gdg_campinas.treffen.model.ScheduleItem;
import org.gdg_campinas.treffen.model.TagMetadata;
import org.gdg_campinas.treffen.provider.ScheduleContract;
import org.gdg_campinas.treffen.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * A {@link ViewHolder} modeling Sessions.
 */
public class SessionItemViewHolder extends ScheduleItemViewHolder
        implements DividerDecoration.Divided {

    private final TextView mTitle;
    private final TextView mReservationStatus;
    private final TextView mDescription;
    private final ViewGroup mTagsHolder;
    private final ImageButton mBookmark;
    private final View mLiveNow;
    private final Button mRate;

    private final Callbacks mCallbacks;
    private final View.OnClickListener mTagClick;
    @Nullable
    private ScheduleItem mSession;

    private SessionItemViewHolder(View itemView, Callbacks callbacks,
                                  SessionTimeFormat timeFormat) {
        super(itemView, timeFormat);
        mCallbacks = callbacks;
        mTitle = (TextView) itemView.findViewById(org.gdg_campinas.treffen.lib.R.id.slot_title);
        mReservationStatus = (TextView) itemView.findViewById(org.gdg_campinas.treffen.lib.R.id.reserve_status);
        mDescription = (TextView) itemView.findViewById(org.gdg_campinas.treffen.lib.R.id.slot_description);
        mTagsHolder = (FlexboxLayout) itemView.findViewById(org.gdg_campinas.treffen.lib.R.id.tags_holder);
        mBookmark = (ImageButton) itemView.findViewById(org.gdg_campinas.treffen.lib.R.id.bookmark);
        mLiveNow = itemView.findViewById(org.gdg_campinas.treffen.lib.R.id.live_now_badge);
        mRate = (Button) itemView.findViewById(org.gdg_campinas.treffen.lib.R.id.give_feedback_button);

        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallbacks == null || mSession == null) {
                    return;
                }
                mCallbacks.onSessionClicked(mSession.sessionId);
            }
        });
        mRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallbacks == null || mSession == null) {
                    return;
                }
                mCallbacks.onFeedbackClicked(mSession.sessionId, mSession.title);
            }
        });
        mBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (mCallbacks == null || mSession == null) {
                    return;
                }
                Resources res = view.getResources();
                // Note: contentDescription is set based on the previous inSchedule state.
                mBookmark.setContentDescription(mSession.inSchedule ?
                        res.getString(org.gdg_campinas.treffen.lib.R.string.add_bookmark) :
                        res.getString(org.gdg_campinas.treffen.lib.R.string.remove_bookmark));
                mBookmark.setActivated(!mBookmark.isActivated());
                mCallbacks.onBookmarkClicked(mSession.sessionId, mSession.inSchedule);
            }
        });
        mTagClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                TagMetadata.Tag tag = (TagMetadata.Tag) v.getTag(org.gdg_campinas.treffen.lib.R.id.key_session_tag);
                if (tag == null || mCallbacks == null) {
                    return;
                }
                mCallbacks.onTagClicked(tag);
            }
        };
    }

    public static SessionItemViewHolder newInstance(ViewGroup parent, Callbacks callbacks,
                                                    SessionTimeFormat timeFormat) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(org.gdg_campinas.treffen.lib.R.layout.schedule_session_item, parent, false);
        return new SessionItemViewHolder(itemView, callbacks, timeFormat);
    }

    private void updateReservationStatus(ScheduleItem item) {
        if (item.reservationStatus == ScheduleContract.MyReservations.RESERVATION_STATUS_RESERVED ||
                item.reservationStatus == ScheduleContract.MyReservations.RESERVATION_STATUS_WAITLISTED) {
            mReservationStatus.setVisibility(VISIBLE);
            mReservationStatus.setCompoundDrawablesWithIntrinsicBounds(
                    item.reservationStatus == ScheduleContract.MyReservations.RESERVATION_STATUS_RESERVED ?
                            org.gdg_campinas.treffen.lib.R.drawable.ic_reserved : org.gdg_campinas.treffen.lib.R.drawable.ic_waitlisted, 0, 0, 0);
            mReservationStatus.setText(item.reservationStatus == ScheduleContract.MyReservations.RESERVATION_STATUS_RESERVED ?
                    org.gdg_campinas.treffen.lib.R.string.schedule_item_reserved : org.gdg_campinas.treffen.lib.R.string.schedule_item_waitlisted);
        } else {
            mReservationStatus.setVisibility(GONE);
        }
    }

    public void bind(@NonNull ScheduleItem item, @NonNull TagPool tagPool,
                     @Nullable TagMetadata tagMetadata) {
        if (item.type != ScheduleItem.SESSION) {
            return;
        }
        mSession = item;
        final Context context = itemView.getContext();

        mTitle.setText(item.title);
        updateReservationStatus(item);
        setDescription(mDescription, item);

        mTagsHolder.removeAllViews();
        if (tagMetadata != null) {
            updateTags(item, tagMetadata, tagPool);
        }

        boolean isLivestreamed = item.isKeynote()
                || (item.flags & ScheduleItem.FLAG_HAS_LIVESTREAM) != 0;
        final long now = TimeUtils.getCurrentTime(context);
        final boolean streamingNow = isLivestreamed && item.startTime <= now && now <= item.endTime;

        if (isLivestreamed && !streamingNow) {
            if (mTagsHolder.getChildCount() > 0) {
                // Insert the spacer first
                mTagsHolder.addView(tagPool.getSpacer(mTagsHolder));
            }
            mTagsHolder.addView(tagPool.getLivestream(mTagsHolder));
        }
        mTagsHolder.setVisibility(mTagsHolder.getChildCount() > 0 ? VISIBLE : GONE);

        if (mCallbacks.bookmarkingEnabled() && !item.isKeynote()) {
            mBookmark.setVisibility(VISIBLE);
            // activated is proxy for in-schedule
            mBookmark.setActivated(item.inSchedule);
        } else {
            mBookmark.setVisibility(GONE);
        }

        mLiveNow.setVisibility(streamingNow ? VISIBLE : GONE);

        boolean showFeedback = mCallbacks.feedbackEnabled()
                && (now >= item.endTime && !item.hasGivenFeedback);
        mRate.setVisibility(showFeedback ? VISIBLE : GONE);
    }

    public void updateTags(@NonNull ScheduleItem item,@NonNull TagMetadata tagMetadata,
                           @NonNull TagPool tagPool) {
        List<TagMetadata.Tag> tags = new ArrayList<>();
        TagMetadata.Tag mainTag = tagMetadata.getTag(item.mainTag);
        if (mainTag != null) tags.add(mainTag);
        for (String tagId : item.tags) {
            if (!tagId.startsWith(Config.Tags.CATEGORY_TRACK) || tagId.equals(item.mainTag)) continue;
            TagMetadata.Tag tag = tagMetadata.getTagById(tagId);
            if (tag != null) tags.add(tag);
        }
        for (TagMetadata.Tag tag : tags) {
            TextView tagView = tagPool.getTag(mTagsHolder);
            tagView.setText(tag.getName());
            tagView.setBackgroundTintList(ColorStateList.valueOf(tag.getColor()));
            tagView.setTag(org.gdg_campinas.treffen.lib.R.id.key_session_tag, tag);
            tagView.setOnClickListener(mTagClick);
            mTagsHolder.addView(tagView);
        }
    }

    public void unbind(TagPool tagPool) {
        if (mTagsHolder.getChildCount() == 0) return;
        for (int i = mTagsHolder.getChildCount() - 1; i >= 0; i--) {
            View view = mTagsHolder.getChildAt(i);
            mTagsHolder.removeViewAt(i);
            if (view instanceof TextView) {
                tagPool.returnTag((TextView) view);
            } else if (view instanceof ImageView) {
                tagPool.returnLivestream((ImageView) view);
            } else {
                tagPool.returnSpacer(view);
            }
        }
    }

    public interface Callbacks {
        /**
         * @param sessionId The ID of the session
         */
        void onSessionClicked(String sessionId);

        /**
         * @return true if bookmark icons should be shown
         */
        boolean bookmarkingEnabled();

        /**
         * @param sessionId    The ID of the session
         * @param isInSchedule Whether the session is bookmarked in the backing data
         */
        void onBookmarkClicked(String sessionId, boolean isInSchedule);

        /**
         * @return true if feedback buttons can be shown
         */
        boolean feedbackEnabled();

        /**
         * @param sessionId    The ID of the session
         * @param sessionTitle The title of the session
         */
        void onFeedbackClicked(String sessionId, String sessionTitle);

        /**
         * @param tag The tag that was clicked
         */
        void onTagClicked(TagMetadata.Tag tag);
    }
}
