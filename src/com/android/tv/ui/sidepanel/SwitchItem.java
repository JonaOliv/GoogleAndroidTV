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

package com.android.tv.ui.sidepanel;

import com.android.tv.R;

public class SwitchItem extends CompoundButtonItem {
    public SwitchItem(String title) {
        super(title, null);
    }

    public SwitchItem(String checkedTitle, String uncheckedTitle) {
        super(checkedTitle, uncheckedTitle, null);
    }

    @Override
    protected int getResourceId() {
        return R.layout.option_item_switch;
    }

    @Override
    protected int getCompoundButtonId() {
        return R.id.switch_button;
    }

    @Override
    protected void onSelected() {
        setChecked(!isChecked());
    }
}
