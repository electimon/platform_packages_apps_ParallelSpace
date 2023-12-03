package com.libremobileos.parallelspace;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;

import androidx.preference.PreferenceFragmentCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.libremobileos.app.ParallelSpaceManager;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppsFragment extends PreferenceFragmentCompat {
    private PreferenceScreen mPreferenceScreen;

    // List of always cloned apps
    private final HashMap<String, String> defaultClonedApps = new HashMap<>() {{
        put("com.android.vending", "com.google.android.finsky.activities.MainActivity");
        put("com.android.documentsui", "com.android.documentsui.LauncherActivity");
    }};

    public static final AppsFragment newInstance(int userId) {
        AppsFragment fragment = new AppsFragment();
        Bundle bundle = new Bundle(1);
        bundle.putInt(AppsActivity.EXTRA_USER_ID, userId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.apps_preferences, rootKey);

        mPreferenceScreen = getPreferenceScreen();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int userId = getArguments().getInt(AppsActivity.EXTRA_USER_ID);
        SpaceAppViewModelFactory spaceAppViewModelFactory = new SpaceAppViewModelFactory(requireActivity().getApplication(), userId);
        final SpaceAppViewModel model = new ViewModelProvider(this, spaceAppViewModelFactory).get(SpaceAppViewModel.class);
        model.getAppList().observeForever(data -> {
            updateAppsList(data);
        });
    }

    private void updateAppsList(List<SpaceAppInfo> apps) {
        int order = 0;
        for (SpaceAppInfo info : apps) {
            SwitchPreference pref = new SwitchPreference(requireActivity());
            pref.setTitle(info.getLabel());
            pref.setSummary(info.getPackageName());
            pref.setIcon(info.getIcon());
            pref.setChecked(info.isAppDuplicated());
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                info.setDuplicateApp((Boolean) newValue);
                return true;
            });
            mPreferenceScreen.addPreference(pref);
            pref.setOrder(order);
            order++;
        }
        for (Map.Entry<String, String> app : defaultClonedApps.entrySet()) {
            ResolveInfo resolveInfo = getResolveInfoFor(getContext(), app.getKey(), app.getValue());
            SpaceAppInfo info = new SpaceAppInfo(resolveInfo,
                    getContext().getPackageManager(),
                    ParallelSpaceManager.getInstance(),
                    getArguments().getInt(AppsActivity.EXTRA_USER_ID),
                    false);
            SwitchPreference pref = new SwitchPreference(requireActivity());
            pref.setTitle(info.getLabel());
            pref.setSummary(info.getPackageName());
            pref.setIcon(info.getIcon());
            pref.setChecked(true);
            pref.setEnabled(false);
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                info.setDuplicateApp((Boolean) newValue);
                return true;
            });
            mPreferenceScreen.addPreference(pref);
            pref.setOrder(order);
            order++;
        }
    }

    private ResolveInfo getResolveInfoFor(Context context, String className, String activityName) {
        Intent intent = new Intent();
        intent.setClassName(className, activityName);
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo;
    }
}
