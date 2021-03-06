/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.dvr.recorder;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.tv.ApplicationSingletons;
import com.android.tv.InputSessionManager;
import com.android.tv.InputSessionManager.OnRecordingSessionChangeListener;
import com.android.tv.R;
import com.android.tv.TvApplication;
import com.android.tv.common.SoftPreconditions;
import com.android.tv.common.feature.CommonFeatures;
import com.android.tv.dvr.WritableDvrDataManager;
import com.android.tv.util.Clock;
import com.android.tv.util.RecurringRunner;

/**
 * DVR Scheduler service.
 *
 * <p> This service is responsible for:
 * <ul>
 *     <li>Send record commands to TV inputs</li>
 *     <li>Wake up at proper timing for recording</li>
 *     <li>Deconflict schedule, handling overlapping times etc.</li>
 *     <li>
 *
 * </ul>
 *
 * <p>The service does not stop it self.
 */
public class DvrRecordingService extends Service {
    private static final String TAG = "DvrRecordingService";
    private static final boolean DEBUG = false;
    public static final String HANDLER_THREAD_NAME = "DvrRecordingService-handler";

    private static final int ONGOING_NOTIFICATION_ID = 1;

    public static void startService(Context context) {
        Intent dvrSchedulerIntent = new Intent(context, DvrRecordingService.class);
        context.startService(dvrSchedulerIntent);
    }

    private final Clock mClock = Clock.SYSTEM;
    private RecurringRunner mReaperRunner;

    private Scheduler mScheduler;
    private HandlerThread mHandlerThread;
    private InputSessionManager mSessionManager;
    private boolean mForeground;

    private final OnRecordingSessionChangeListener mOnRecordingSessionChangeListener =
            new OnRecordingSessionChangeListener() {
                @Override
                public void onRecordingSessionChange(final boolean create, final int count) {
                    if (create && !mForeground) {
                        Notification notification =
                                new Notification.Builder(getApplicationContext())
                                        .setContentTitle(TAG)
                                        .setSmallIcon(R.drawable.ic_dvr)
                                        .build();
                        startForeground(ONGOING_NOTIFICATION_ID, notification);
                        mForeground = true;
                    } else if (!create && mForeground && count == 0) {
                        stopForeground(STOP_FOREGROUND_REMOVE);
                        mForeground = false;
                    }
                }
            };

    @Override
    public void onCreate() {
        TvApplication.setCurrentRunningProcess(this, true);
        if (DEBUG) Log.d(TAG, "onCreate");
        super.onCreate();
        SoftPreconditions.checkFeatureEnabled(this, CommonFeatures.DVR, TAG);
        ApplicationSingletons singletons = TvApplication.getSingletons(this);
        WritableDvrDataManager dataManager =
                (WritableDvrDataManager) singletons.getDvrDataManager();
        mSessionManager = singletons.getInputSessionManager();

        mSessionManager.addOnRecordingSessionChangeListener(mOnRecordingSessionChangeListener);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // mScheduler may have been set for testing.
        if (mScheduler == null) {
            mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
            mHandlerThread.start();
            mScheduler = new Scheduler(mHandlerThread.getLooper(), singletons.getDvrManager(),
                    singletons.getInputSessionManager(), dataManager,
                    singletons.getChannelDataManager(), singletons.getTvInputManagerHelper(), this,
                    mClock, alarmManager);
            mScheduler.start();
        }
        mReaperRunner = new RecurringRunner(this, java.util.concurrent.TimeUnit.DAYS.toMillis(1),
                new ScheduledProgramReaper(dataManager, mClock), null);
        mReaperRunner.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand (" + intent + "," + flags + "," + startId + ")");
        mScheduler.update();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        mReaperRunner.stop();
        mScheduler.stop();
        mScheduler = null;
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
        mSessionManager.removeRecordingSessionChangeListener(mOnRecordingSessionChangeListener);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @VisibleForTesting
    void setScheduler(Scheduler scheduler) {
        Log.i(TAG, "Setting scheduler for tests to " + scheduler);
        mScheduler = scheduler;
    }
}
