/*
 * Copyright (c) 2016 Google Inc.
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

package org.gdg_campinas.treffen.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import org.gdg_campinas.treffen.archframework.PresenterImpl;
import org.gdg_campinas.treffen.archframework.UpdatableView;
import org.gdg_campinas.treffen.navigation.NavigationModel.NavigationQueryEnum;
import org.gdg_campinas.treffen.navigation.NavigationModel.NavigationUserActionEnum;

/**
 * This abstract class implements both {@link UpdatableView} and {@link AppNavigationView}, without
 * any specific UI implementation details. This uses the {@link org.gdg_campinas.treffen
 * .archframework} for getting its data and processing user actions. Some methods which are UI
 * specific are left abstract. Extend this class for full navigation functionality.
 */
public abstract class AppNavigationViewAbstractImpl implements
        UpdatableView<NavigationModel, NavigationModel.NavigationQueryEnum, NavigationModel.NavigationUserActionEnum>,
        AppNavigationView {

    private static final long BOTTOM_NAV_ANIM_GRACE = 115L;
    private UserActionListener<NavigationModel.NavigationUserActionEnum> mUserActionListener;

    protected Activity mActivity;

    private final Handler mHandler = new Handler();

    protected NavigationModel.NavigationItemEnum mSelfItem;

    @Override
    public void displayData(final NavigationModel model, final NavigationModel.NavigationQueryEnum query) {
        switch (query) {
            case LOAD_ITEMS:
                displayNavigationItems(model.getItems());
                break;
        }
    }

    @Override
    public void displayErrorMessage(final NavigationModel.NavigationQueryEnum query) {
        switch (query) {
            case LOAD_ITEMS:
                // No error message displayed
                break;
        }
    }

    @Override
    public void activityReady(Activity activity, NavigationModel.NavigationItemEnum self) {
        mActivity = activity;
        mSelfItem = self;

        setUpView();

        NavigationModel model = new NavigationModel();
        PresenterImpl<NavigationModel, NavigationQueryEnum, NavigationUserActionEnum> presenter
                = new PresenterImpl<>(model, this, NavigationModel.NavigationUserActionEnum.values(),
                NavigationModel.NavigationQueryEnum.values());
        presenter.loadInitialQueries();
        addListener(presenter);
    }

    @Override
    public void updateNavigationItems() {
        mUserActionListener.onUserAction(NavigationModel.NavigationUserActionEnum.RELOAD_ITEMS, null);
    }

    @Override
    public abstract void displayNavigationItems(final NavigationModel.NavigationItemEnum[] items);

    @Override
    public abstract void setUpView();

    @Override
    public abstract void showNavigation();

    @Override
    public void itemSelected(final NavigationModel.NavigationItemEnum item) {
        if (item.getClassToLaunch() != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.startActivity(new Intent(mActivity, item.getClassToLaunch()));
                    if (item.finishCurrentActivity()) {
                        mActivity.finish();
                        mActivity.overridePendingTransition(org.gdg_campinas.treffen.lib.R.anim.fade_in, org.gdg_campinas.treffen.lib.R.anim.fade_out);
                    }
                }
            }, BOTTOM_NAV_ANIM_GRACE);
        }
    }

    @Override
    public void displayUserActionResult(final NavigationModel model,
                                        final NavigationModel.NavigationUserActionEnum userAction, final boolean success) {
        switch (userAction) {
            case RELOAD_ITEMS:
                displayNavigationItems(model.getItems());
                break;
        }
    }

    @Override
    public Uri getDataUri(final NavigationModel.NavigationQueryEnum query) {
        // This feature has no Uri
        return null;
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public void addListener(final UserActionListener<NavigationModel.NavigationUserActionEnum> listener) {
        mUserActionListener = listener;
    }
}
