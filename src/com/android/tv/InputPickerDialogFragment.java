/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.tv.TvInputInfo;
import android.tv.TvInputManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Insert description here. (generated by jaeseo)
 */
public class InputPickerDialogFragment extends DialogFragment {
    public static final String DIALOG_TAG = InputPickerDialogFragment.class.getName();

    private static final String TAG = "InputPickerDialogFragment";
    private static final String DIALOG_EDIT_INPUT = "edit_input";

    private final Map<String, TvInput> mInputMap = new HashMap<String, TvInput>();
    private final Map<String, Boolean> mInputAvailabilityMap =
            new HashMap<String, Boolean>();

    private String mSelectedInputId;

    // The first item of the adapter is always 'Unified TV input'.
    private ArrayAdapter<String> mAdapter;
    private InputPickerDialogListener mListener;

    private TvInputManager mTvInputManager;

    private final TvInputManager.TvInputListener mAvailabilityListener =
            new TvInputManager.TvInputListener() {
                @Override
                public void onAvailabilityChanged(String inputId, boolean isAvailable) {
                    mInputAvailabilityMap.put(inputId, Boolean.valueOf(isAvailable));
                    mAdapter.notifyDataSetChanged();
                }
            };

    private final Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TvInput selectedTvInput = ((TvActivity) getActivity()).getSelectedTvInput();
        mSelectedInputId = selectedTvInput != null ? selectedTvInput.getId() : null;
        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
                new ArrayList<String>()) {
            @Override
            public boolean areAllItemsEnabled() {
                // Some inputs might not be available at the moment.
                return false;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                view.setEnabled(isEnabled(position));
                return view;
            }

            @Override
            public boolean isEnabled(int position) {
                TvInput input = mInputMap.get(mAdapter.getItem(position));
                return !input.getId().equals(mSelectedInputId) && input.isAvailable();
            }
        };
        mTvInputManager = (TvInputManager) getActivity().getSystemService(Context.TV_INPUT_SERVICE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_input_device)
                .setAdapter(mAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TvInput input = mInputMap.get(mAdapter.getItem(which));
                        mListener.onInputPicked(input);
                    }
                })
                .setNeutralButton(R.string.edit_input_device_name,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                Fragment prev = getFragmentManager().findFragmentByTag(
                                        DIALOG_EDIT_INPUT);
                                if (prev != null) {
                                    ft.remove(prev);
                                }
                                ft.addToBackStack(null);
                                DialogFragment fragment = new EditInputDialogFragment();
                                fragment.show(ft, DIALOG_EDIT_INPUT);
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ((AlertDialog) dialog).getButton(Dialog.BUTTON_NEUTRAL)
                        .setEnabled(mAdapter.getCount() > 0);
            }
        });
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface.
        try {
            // Instantiate the InputPickerDialogListener so we can send events to the host.
            mListener = (InputPickerDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception.
            throw new ClassCastException(
                    activity.toString() + " must implement InputPickerDialogListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupInputAdapter();
    }

    private void setupInputAdapter() {
        mInputMap.clear();
        mInputAvailabilityMap.clear();
        mAdapter.clear();

        TvInputManagerHelper inputManagerHelper =
                ((TvActivity) getActivity()).getTvInputManagerHelper();

        if (inputManagerHelper.getTvInputSize() < 1) {
            ((AlertDialog) getDialog()).getButton(Dialog.BUTTON_NEUTRAL).setEnabled(false);
            return;
        }

        for (TvInputInfo inputInfo : inputManagerHelper.getTvInputInfos(false)) {
            TvInput input = new TisTvInput(inputManagerHelper, inputInfo, getActivity());
            String name = input.getDisplayName();
            if (input.getId().equals(mSelectedInputId)) {
                name += " " + getResources().getString(R.string.selected);
            }
            mInputMap.put(name, input);
            mTvInputManager.registerListener(input.getId(), mAvailabilityListener, mHandler);
        }

        String[] inputStrings = mInputMap.keySet().toArray(new String[0]);
        Arrays.sort(inputStrings);

        TvInput unifiedTvInput = new UnifiedTvInput(inputManagerHelper, getActivity());
        String unifiedTvInputName = unifiedTvInput.getDisplayName();
        if (unifiedTvInput.getId().equals(mSelectedInputId)) {
            unifiedTvInputName += " " + getResources().getString(R.string.selected);
        }
        mInputMap.put(unifiedTvInputName, unifiedTvInput);

        // Unified TV input is always the first item.
        mAdapter.add(unifiedTvInputName);
        mAdapter.addAll(inputStrings);
        mAdapter.notifyDataSetChanged();

        ((AlertDialog) getDialog()).getButton(Dialog.BUTTON_NEUTRAL).setEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        List<TvInputInfo> inputs = mTvInputManager.getTvInputList();
        if (inputs.size() > 0) {
            for (TvInputInfo input : inputs) {
                mTvInputManager.unregisterListener(input.getId(), mAvailabilityListener);
            }
        }
    }

    public interface InputPickerDialogListener {
        public void onInputPicked(TvInput input);
    }
}
