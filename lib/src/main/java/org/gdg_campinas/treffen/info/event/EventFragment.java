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
package org.gdg_campinas.treffen.info.event;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.gdg_campinas.treffen.map.MapActivity;
import org.gdg_campinas.treffen.util.LogUtils;
import org.gdg_campinas.treffen.info.BaseInfoFragment;
import org.gdg_campinas.treffen.lib.R;
import org.gdg_campinas.treffen.schedule.ScheduleActivity;
import org.gdg_campinas.treffen.util.WiFiUtils;

import static org.gdg_campinas.treffen.util.LogUtils.LOGE;
import static org.gdg_campinas.treffen.util.LogUtils.makeLogTag;

public class EventFragment extends BaseInfoFragment<EventInfo> implements EventView.EventViewClickListener {
    private static final String TAG = LogUtils.makeLogTag(EventFragment.class);

    private EventInfo mEventInfo;

    private TextView mWiFiNetworkText;
    private TextView mWiFiPasswordText;
    private Button mWiFiSave;
    private EventView mSandboxEventContent;
    private EventView mCodeLabsEventContent;
    private EventView mOfficeHoursEventContent;
    private EventView mAfterHoursEventContent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.info_event_frag, container, false);
        mWiFiNetworkText = (TextView) root.findViewById(R.id.wifi_network_value);
        mWiFiPasswordText = (TextView) root.findViewById(R.id.wifi_password_value);
        mWiFiSave = (Button) root.findViewById(R.id.wifi_save);
        mWiFiSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean saved = WiFiUtils.installConferenceWiFi(getContext());
                Snackbar.make(getView(), saved ? R.string.wifi_install_success
                        : R.string.wifi_install_error_message, Snackbar.LENGTH_SHORT).show();
            }
        });
        mSandboxEventContent = (EventView) root.findViewById(R.id.sandbox_event);
        mCodeLabsEventContent = (EventView) root.findViewById(R.id.codelabs_event);
        mOfficeHoursEventContent = (EventView) root.findViewById(R.id.officehours_event);
        mAfterHoursEventContent = (EventView) root.findViewById(R.id.afterhours_event);
        mSandboxEventContent.setEventViewClickListener(this);
        mCodeLabsEventContent.setEventViewClickListener(this);
        mOfficeHoursEventContent.setEventViewClickListener(this);
        mAfterHoursEventContent.setEventViewClickListener(this);
        return root;
    }

    @Override
    public String getTitle(@NonNull Resources resources) {
        return resources.getString(R.string.title_event);
    }

    @Override
    public void updateInfo(EventInfo info) {
        mEventInfo = info;
    }

    @Override
    protected void showInfo() {
        if (mEventInfo != null) {
            mWiFiNetworkText.setText(mEventInfo.getWiFiNetwork());
            mWiFiPasswordText.setText(mEventInfo.getWiFiPassword());
            mSandboxEventContent.setEventDescription(mEventInfo.getSandboxDescription());
            mCodeLabsEventContent.setEventDescription(mEventInfo.getCodeLabsDescription());
            mOfficeHoursEventContent.setEventDescription(mEventInfo.getOfficeHoursDescription());
            mAfterHoursEventContent.setEventDescription(mEventInfo.getAfterHoursDescription());
        } else {
            LogUtils.LOGE(TAG, "EventInfo should not be null.");
        }
    }

    @Override
    public void onViewSessionsClicked(EventView view, String filterTag) {
        ScheduleActivity.launchScheduleWithFilterTag(view.getContext(), filterTag);
        getActivity().finish();
    }

    @Override
    public void onViewMapClicked(EventView view, String mapUri) {
        final Activity activity = getActivity();
        final Intent mapIntent = new Intent(activity, MapActivity.class);
        if (mapUri != null) {
            mapIntent.putExtra(MapActivity.EXTRA_ROOM, mapUri);
        }
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(mapIntent);
        activity.finish();
    }
}
