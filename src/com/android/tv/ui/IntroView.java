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
 * limitations under the License.
 */

package com.android.tv.ui;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import com.android.tv.R;
import com.android.tv.menu.MenuView;

public class IntroView extends FullscreenDialogView {
    private AnimationDrawable mRippleDrawable;
    private boolean mOpenMenu;

    public IntroView(Context context) {
        this(context, null, 0);
    }

    public IntroView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IntroView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTransitionAnimationEnabled(false);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    dismiss();
                    mOpenMenu = true;
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        dismiss();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        View v = findViewById(R.id.welcome_ripple);
        mRippleDrawable = (AnimationDrawable) v.getBackground();
        mRippleDrawable.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        mRippleDrawable.stop();
        super.onDetachedFromWindow();
    }

    @Override
    public void onDestroy() {
        if (mOpenMenu) {
            getActivity().getOverlayManager().showMenu(MenuView.REASON_GUIDE);
        }
    }
}
