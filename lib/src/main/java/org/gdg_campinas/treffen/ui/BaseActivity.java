/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package org.gdg_campinas.treffen.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;

import org.gdg_campinas.treffen.feed.FeedState;
import org.gdg_campinas.treffen.lib.BuildConfig;
import org.gdg_campinas.treffen.navigation.AppNavigationView;
import org.gdg_campinas.treffen.navigation.AppNavigationViewAsBottomNavImpl;
import org.gdg_campinas.treffen.navigation.NavigationModel;
import org.gdg_campinas.treffen.provider.ScheduleContract;
import org.gdg_campinas.treffen.service.DataBootstrapService;
import org.gdg_campinas.treffen.sync.SyncHelper;
import org.gdg_campinas.treffen.sync.account.Account;
import org.gdg_campinas.treffen.ui.widget.BadgedBottomNavigationView;
import org.gdg_campinas.treffen.ui.widget.MultiSwipeRefreshLayout;
import org.gdg_campinas.treffen.util.AccountUtils;
import org.gdg_campinas.treffen.util.AnalyticsHelper;
import org.gdg_campinas.treffen.util.ConnectivityUtils;
import org.gdg_campinas.treffen.util.LogUtils;
import org.gdg_campinas.treffen.util.RecentTasksStyler;
import org.gdg_campinas.treffen.welcome.WelcomeActivity;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * A base activity that handles common functionality in the app. This includes the navigation
 * drawer, login and authentication, Action Bar tweaks, amongst others.
 */
public abstract class BaseActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        MultiSwipeRefreshLayout.CanChildScrollUpCallback {

    private static final String TAG = LogUtils.makeLogTag(BaseActivity.class);
    // Navigation drawer
    private AppNavigationView mAppNavigationView;
    // Toolbar
    private Toolbar mToolbar;
    private TextView mToolbarTitle;
    // SwipeRefreshLayout allows the user to swipe the screen down to trigger a manual refresh
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // handle to our sync observer (that notifies us about changes in our sync state)
    private Object mSyncObserverHandle;
    private MySyncStatusObserver mSyncStatusObserver = new MySyncStatusObserver();

    /**
     * This utility method handles Up navigation intents by searching for a parent activity and
     * navigating there if defined. When using this for an activity make sure to define both the
     * native parentActivity as well as the AppCompat one when supporting API levels less than 16.
     * when the activity has a single parent activity. If the activity doesn't have a single parent
     * activity then don't define one and this method will use back button functionality. If "Up"
     * functionality is still desired for activities without parents then use {@code
     * syntheticParentActivity} to define one dynamically.
     * <p/>
     * Note: Up navigation intents are represented by a back arrow in the top left of the Toolbar in
     * Material Design guidelines.
     *
     * @param currentActivity         Activity in use when navigate Up action occurred.
     * @param syntheticParentActivity Parent activity to use when one is not already configured.
     */
    public static void navigateUpOrBack(Activity currentActivity,
            Class<? extends Activity> syntheticParentActivity) {
        // Retrieve parent activity from AndroidManifest.
        Intent intent = NavUtils.getParentActivityIntent(currentActivity);

        // Synthesize the parent activity when a natural one doesn't exist.
        if (intent == null && syntheticParentActivity != null) {
            try {
                intent = NavUtils.getParentActivityIntent(currentActivity, syntheticParentActivity);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (intent == null) {
            // No parent defined in manifest. This indicates the activity may be used by
            // in multiple flows throughout the app and doesn't have a strict parent. In
            // this case the navigation up button should act in the same manner as the
            // back button. This will result in users being forwarded back to other
            // applications if currentActivity was invoked from another application.
            currentActivity.onBackPressed();
        } else {
            if (NavUtils.shouldUpRecreateTask(currentActivity, intent)) {
                // Need to synthesize a backstack since currentActivity was probably invoked by a
                // different app. The preserves the "Up" functionality within the app according to
                // the activity hierarchy defined in AndroidManifest.xml via parentActivity
                // attributes.
                TaskStackBuilder builder = TaskStackBuilder.create(currentActivity);
                builder.addNextIntentWithParentStack(intent);
                builder.startActivities();
            } else {
                // Navigate normally to the manifest defined "Up" activity.
                NavUtils.navigateUpTo(currentActivity, intent);
            }
        }
    }

    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
     */
    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecentTasksStyler.styleRecentTasksEntry(this);

        // Check if the EULA has been accepted; if not, show it.
        if (WelcomeActivity.shouldDisplay(this)) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        Account.createSyncAccount(this);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        String screenLabel = getAnalyticsScreenLabel();
        if (screenLabel != null) {
            AnalyticsHelper.sendScreenView(screenLabel, this);
        }
    }

    private void trySetupSwipeRefresh() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(org.gdg_campinas.treffen.lib.R.id.swipe_refresh_layout);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setColorSchemeResources(org.gdg_campinas.treffen.lib.R.color.sunflower_yellow, org.gdg_campinas.treffen.lib.R.color.neon_blue,
                    org.gdg_campinas.treffen.lib.R.color.lightish_blue, org.gdg_campinas.treffen.lib.R.color.aqua_marine);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (ConnectivityUtils.isConnected(BaseActivity.this)) {
                        requestDataRefresh();
                    } else {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }
            });

            if (mSwipeRefreshLayout instanceof MultiSwipeRefreshLayout) {
                MultiSwipeRefreshLayout mswrl = (MultiSwipeRefreshLayout) mSwipeRefreshLayout;
                mswrl.setCanChildScrollUpCallback(this);
            }
        }
    }

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses of
     * BaseActivity override this to indicate what nav drawer item corresponds to them Return
     * NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.INVALID;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getToolbar();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (BuildConfig.PREF_ATTENDEE_AT_VENUE.equals(key)) {
            LogUtils.LOGD(TAG, "Attendee at venue preference changed, repopulating nav drawer and menu.");
            if (mAppNavigationView != null) {
                mAppNavigationView.updateNavigationItems();
            }
            invalidateOptionsMenu();
        } else if (BuildConfig.NEW_FEED_ITEM.equals(key)) {
            LogUtils.LOGD(TAG, "New feed message detected - SharedPrefs");
            // Update the Feed icon badge every time there is a change to SharedPrefs; it is
            // updated by the FCM service every time a new Feed message is received, and reset
            // every time the Feed page is visited.
            updateFeedBadge();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final BadgedBottomNavigationView bottomNav = (BadgedBottomNavigationView)
                findViewById(org.gdg_campinas.treffen.lib.R.id.bottom_navigation);
        if (bottomNav != null) {
            mAppNavigationView = new AppNavigationViewAsBottomNavImpl(bottomNav);
            mAppNavigationView.activityReady(this, getSelfNavDrawerItem());
            // Since onPostCreate happens after onStart, we can't badge during onStart when the
            // Activity is launched because the AppNavigationView isn't instantiated until this
            // point.
            updateFeedBadge();
        }

        trySetupSwipeRefresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == org.gdg_campinas.treffen.lib.R.id.menu_refresh) {
            requestDataRefresh();

        }
        return super.onOptionsItemSelected(item);
    }

    public void updateFeedBadge() {
        if (FeedState.getInstance().isNewFeedItem(this)) {
            showFeedBadge();
        } else {
            clearFeedBadge();
        }
    }

    protected void requestDataRefresh() {
        android.accounts.Account activeAccount = AccountUtils.getActiveAccount(this);
        if (activeAccount == null) {
            return;
        }
        if (ContentResolver.isSyncActive(activeAccount, ScheduleContract.CONTENT_AUTHORITY)) {
            LogUtils.LOGD(TAG, "Ignoring manual sync request because a sync is already in progress.");
            return;
        }
        LogUtils.LOGD(TAG, "Requesting manual data refresh.");
        SyncHelper.requestManualSync();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Perform one-time bootstrap setup, if needed
        DataBootstrapService.startDataBootstrapIfNecessary(this);

        if (mSyncStatusObserver == null) {
            mSyncStatusObserver = new MySyncStatusObserver();
        }
        mSyncStatusObserver.register(this);
        // Watch for sync state changes
        mSyncStatusObserver.onStatusChanged(0);
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
        if (mSyncStatusObserver != null) {
            mSyncStatusObserver.unregister();
            mSyncStatusObserver = null;
        }
    }

    @Override
    protected void onDestroy() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public Toolbar getToolbar() {
        if (mToolbar == null) {
            mToolbar = (Toolbar) findViewById(org.gdg_campinas.treffen.lib.R.id.toolbar);
            if (mToolbar != null) {
                setSupportActionBar(mToolbar);
                mToolbar.setNavigationContentDescription(org.gdg_campinas.treffen.lib.R.string.navdrawer_description_a11y);
                mToolbarTitle = (TextView) mToolbar.findViewById(org.gdg_campinas.treffen.lib.R.id.toolbar_title);
                if (mToolbarTitle != null) {
                    int titleId = getNavigationTitleId();
                    if (titleId != 0) {
                        mToolbarTitle.setText(titleId);
                    }
                }

                // We use our own toolbar title, so hide the default one
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        }
        return mToolbar;
    }

    /**
     * @param clickListener The {@link android.view.View.OnClickListener} for the navigation icon of
     *                      the toolbar.
     */
    protected void setToolbarAsUp(View.OnClickListener clickListener) {
        // Initialise the toolbar
        getToolbar();
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(org.gdg_campinas.treffen.lib.R.drawable.ic_up);
            mToolbar.setNavigationContentDescription(org.gdg_campinas.treffen.lib.R.string.close_and_go_back);
            mToolbar.setNavigationOnClickListener(clickListener);
        }
    }

    @Override
    protected void onStart() {
        // Update feed badge during onStart because if the app receives the Feed FCM (and updates
        // the FeedState) while the Activity is no longer visible, this will update the badge
        // when the Activity is visible again.
        updateFeedBadge();
        super.onStart();
    }

    protected void onRefreshingStateChanged(boolean refreshing) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return false;
    }

    protected String getAnalyticsScreenLabel() {
        return null;
    }

    protected int getNavigationTitleId() {
        return 0;
    }

    protected void setFullscreenLayout() {
        View decor = getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decor.setSystemUiVisibility(flags);
    }

    protected void showFeedBadge() {
        if (mAppNavigationView != null) {
            mAppNavigationView.showItemBadge(NavigationModel.NavigationItemEnum.FEED);
        }
    }

    protected void clearFeedBadge() {
        if (mAppNavigationView != null) {
            mAppNavigationView.clearItemBadge(NavigationModel.NavigationItemEnum.FEED);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    private static class MySyncStatusObserver implements SyncStatusObserver {
        private BaseActivity mActivity;

        public void register(BaseActivity activity) {
            mActivity = activity;
        }

        public void unregister() {
            mActivity = null;
        }


        @Override
        public void onStatusChanged(int which) {
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mActivity == null) {
                            return;
                        }
                        String accountName = AccountUtils.getActiveAccountName(mActivity);
                        if (TextUtils.isEmpty(accountName)) {
                            mActivity.onRefreshingStateChanged(false);
                            return;
                        }
                        android.accounts.Account account = new android.accounts.Account(
                                accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
                        boolean syncActive = ContentResolver.isSyncActive(
                                account, ScheduleContract.CONTENT_AUTHORITY);
                        mActivity.onRefreshingStateChanged(syncActive);
                    }
                });
            }
        }
    }

}
