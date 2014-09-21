/*
 * Copyright (C) 2013 The CyanogenMod project
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

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_BLUETOOTH_INPUT_SETTINGS = "bluetooth_input_settings";

    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_CAMERA = 0x20;

    private CheckBoxPreference mCameraWake;
    private CheckBoxPreference mCameraSleepOnRelease;
    private CheckBoxPreference mCameraMusicControls;
    private ListPreference mVolumeKeyCursorControl;
    private CheckBoxPreference mSwapVolumeButtons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final boolean hasCameraKey = (deviceKeys & KEY_MASK_CAMERA) != 0;

        boolean hasAnyBindableKey = false;
        final PreferenceCategory cameraCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_CAMERA);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        if (hasCameraKey) {
            mCameraWake = (CheckBoxPreference)
                prefScreen.findPreference(Settings.System.CAMERA_WAKE_SCREEN);
            mCameraSleepOnRelease = (CheckBoxPreference)
                prefScreen.findPreference(Settings.System.CAMERA_SLEEP_ON_RELEASE);
            mCameraMusicControls = (CheckBoxPreference)
                prefScreen.findPreference(Settings.System.CAMERA_MUSIC_CONTROLS);
            boolean value = mCameraWake.isChecked();
            mCameraMusicControls.setEnabled(!value);
            mCameraSleepOnRelease.setEnabled(value);
            if (getResources().getBoolean(
                com.android.internal.R.bool.config_singleStageCameraKey)) {
                cameraCategory.removePreference(mCameraSleepOnRelease);
            }
        } else {
            prefScreen.removePreference(cameraCategory);
        }

        if (Utils.hasVolumeRocker(getActivity())) {
            int swapVolumeKeys = Settings.System.getInt(getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = (CheckBoxPreference)
                    prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);

            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initActionList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);
        } else {
            prefScreen.removePreference(volumeCategory);
        }

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_BLUETOOTH_INPUT_SETTINGS);
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);

        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeKeyCursorControl) {
            handleActionListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        }

        return false;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSwapVolumeButtons) {
            int value = mSwapVolumeButtons.isChecked()
                    ? (Utils.isTablet(getActivity()) ? 2 : 1) : 0;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
        } else if (preference == mCameraWake) {
            // Disable camera music controls if camera wake is enabled
            boolean isCameraWakeEnabled = mCameraWake.isChecked();
            mCameraMusicControls.setEnabled(!isCameraWakeEnabled);
            mCameraSleepOnRelease.setEnabled(isCameraWakeEnabled);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
