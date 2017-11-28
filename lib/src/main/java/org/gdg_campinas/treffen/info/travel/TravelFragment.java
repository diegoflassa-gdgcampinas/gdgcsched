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
package org.gdg_campinas.treffen.info.travel;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.gdg_campinas.treffen.info.BaseInfoFragment;
import org.gdg_campinas.treffen.info.CollapsibleCard;
import org.gdg_campinas.treffen.util.LogUtils;

public class TravelFragment extends BaseInfoFragment<TravelInfo> {
    private static final String TAG = LogUtils.makeLogTag(TravelFragment.class);

    private TravelInfo mTravelInfo;

    private CollapsibleCard bikingCard;
    private CollapsibleCard shuttleServiceCard;
    private CollapsibleCard carpoolingParkingCard;
    private CollapsibleCard publicTransportationCard;
    private CollapsibleCard rideSharingCard;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(org.gdg_campinas.treffen.lib.R.layout.info_travel_frag, container, false);
        bikingCard = (CollapsibleCard) root.findViewById(org.gdg_campinas.treffen.lib.R.id.bikingCard);
        shuttleServiceCard = (CollapsibleCard) root.findViewById(org.gdg_campinas.treffen.lib.R.id.shuttleInfoCard);
        carpoolingParkingCard = (CollapsibleCard) root.findViewById(org.gdg_campinas.treffen.lib.R.id.carpoolingParkingCard);
        publicTransportationCard =
                (CollapsibleCard) root.findViewById(org.gdg_campinas.treffen.lib.R.id.publicTransportationCard);
        rideSharingCard = (CollapsibleCard) root.findViewById(org.gdg_campinas.treffen.lib.R.id.rideSharingCard);
        return root;
    }

    @Override
    public String getTitle(@NonNull Resources resources) {
        return resources.getString(org.gdg_campinas.treffen.lib.R.string.title_travel);
    }

    @Override
    public void updateInfo(TravelInfo info) {
        mTravelInfo = info;
    }

    @Override
    protected void showInfo() {
        if (mTravelInfo != null) {
            bikingCard.setCardDescription(mTravelInfo.getBikingInfo());
            shuttleServiceCard.setCardDescription(mTravelInfo.getShuttleInfo());
            carpoolingParkingCard.setCardDescription(mTravelInfo.getCarpoolingParkingInfo());
            publicTransportationCard.setCardDescription(mTravelInfo.getPublicTransportationInfo());
            rideSharingCard.setCardDescription(mTravelInfo.getRideSharingInfo());
        } else {
            LogUtils.LOGE(TAG, "TravelInfo should not be null.");
        }
    }
}
