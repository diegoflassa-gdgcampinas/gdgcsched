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
package org.gdg_campinas.treffen.info;

import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.gdg_campinas.treffen.info.about.AboutInfo;
import org.gdg_campinas.treffen.info.event.EventInfo;
import org.gdg_campinas.treffen.schedule.ScheduleSingleDayFragment;
import org.gdg_campinas.treffen.util.AnalyticsHelper;
import org.gdg_campinas.treffen.info.travel.TravelInfo;

public class InfoPagerFragment extends Fragment implements InfoContract.View {

    /**
     * The key used to save the tags for {@link ScheduleSingleDayFragment}s so the automatically
     * recreated fragments can be reused by {@link #mViewPagerAdapter}.
     */
    private static final String INFO_TAB_FRAGMENTS_TAGS = "info_tab_fragments_tags";

    /**
     * The key used to save the position in the {@link #mViewPagerAdapter} for the current {@link
     * ScheduleSingleDayFragment}s.
     */
    private static final String CURRENT_INFO_TAB_FRAGMENT_POSITION =
            "current_single_day_fragments_position";

    private ViewPager mViewPager;
    private InfoViewPagerAdapter mViewPagerAdapter;
    private TabLayout mTabLayout;
    private int mCurrentPage;
    private InfoContract.Presenter mPresenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(org.gdg_campinas.treffen.lib.R.layout.info_pager_frag, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String[] infoTabFragmentTags = null;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(INFO_TAB_FRAGMENTS_TAGS)) {
                infoTabFragmentTags = savedInstanceState.getStringArray(
                        INFO_TAB_FRAGMENTS_TAGS);
            }
            if (savedInstanceState.containsKey(CURRENT_INFO_TAB_FRAGMENT_POSITION)) {
                mCurrentPage = savedInstanceState.getInt(
                        CURRENT_INFO_TAB_FRAGMENT_POSITION);
            }
        }
        mViewPager = (ViewPager) view.findViewById(org.gdg_campinas.treffen.lib.R.id.view_pager);
        mViewPagerAdapter = new InfoViewPagerAdapter(getContext(),
                getChildFragmentManager());
        mViewPagerAdapter.setRetainedFragmentsTags(infoTabFragmentTags);
        mViewPager.setAdapter(mViewPagerAdapter);
        mTabLayout = (TabLayout) view.findViewById(org.gdg_campinas.treffen.lib.R.id.sliding_tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        String currentLabel = (String) mTabLayout.getTabAt(mViewPager.getCurrentItem()).getText();
        AnalyticsHelper.sendScreenView("Info: " + currentLabel, getActivity());

        // Add a listener for any reselection events
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(final TabLayout.Tab tab) {
                AnalyticsHelper.sendScreenView("Info: " + tab.getText().toString(), getActivity());
            }

            @Override
            public void onTabUnselected(final TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(final TabLayout.Tab tab) {
            }
        });

        mViewPager.setPageMargin(getResources()
                .getDimensionPixelSize(org.gdg_campinas.treffen.lib.R.dimen.my_schedule_page_margin));
        mViewPager.setPageMarginDrawable(org.gdg_campinas.treffen.lib.R.drawable.page_margin);
        View header = view.findViewById(org.gdg_campinas.treffen.lib.R.id.header_anim);
        if (header instanceof ImageView) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) ContextCompat.getDrawable(
                    getContext(), org.gdg_campinas.treffen.lib.R.drawable.avd_header_info);
            ((ImageView) header).setImageDrawable(avd);
            avd.start();
        }

        setCurrentPage();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mViewPagerAdapter != null && mViewPagerAdapter.getFragments() != null) {
            BaseInfoFragment[] infoFragments = mViewPagerAdapter.getFragments();
            String[] tags = new String[infoFragments.length];
            for (int i = 0; i < tags.length; i++) {
                tags[i] = infoFragments[i].getTag();
            }
            outState.putStringArray(INFO_TAB_FRAGMENTS_TAGS, tags);
            outState.putInt(CURRENT_INFO_TAB_FRAGMENT_POSITION, mViewPager.getCurrentItem());
        }
    }

    private void setCurrentPage() {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(mCurrentPage);
        }
    }

    @Override
    public void setPresenter(InfoContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showEventInfo(EventInfo eventInfo) {
        mViewPagerAdapter.updateEventInfo(eventInfo);
    }

    @Override
    public void showTravelInfo(TravelInfo travelInfo) {
        mViewPagerAdapter.updateTravelInfo(travelInfo);
    }

    @Override
    public void showAboutInfo(AboutInfo aboutInfo) {
        mViewPagerAdapter.updateAboutInfo(aboutInfo);
    }
}
