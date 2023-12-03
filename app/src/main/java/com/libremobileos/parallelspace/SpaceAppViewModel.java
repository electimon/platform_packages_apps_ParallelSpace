package com.libremobileos.parallelspace;

import static com.android.internal.util.libremobileos.PackageManagerUtils.getApplicationInfo;
import static com.android.internal.util.libremobileos.PackageManagerUtils.isAppEnabled;
import static com.android.internal.util.libremobileos.PackageManagerUtils.isAppInstalled;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Space;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.android.internal.libremobileos.app.ParallelSpaceManager;

import java.util.ArrayList;
import java.util.List;

public class SpaceAppViewModel extends AndroidViewModel {
    private final SpaceAppListLiveData mLiveData;

    public SpaceAppViewModel(Application application, int userId) {
        super(application);
        mLiveData = new SpaceAppListLiveData(application, userId);
    }

    public LiveData<List<SpaceAppInfo>> getAppList() {
        return mLiveData;
    }
}

class SpaceAppListLiveData extends LiveData<List<SpaceAppInfo>> {

    private final PackageManager mPackageManager;
    private final ParallelSpaceManager mParallelSpaceManager;
    private final Context mContext;
    private int mCurrentDataVersion;

    public SpaceAppListLiveData(Context context, int userId) {
        mPackageManager = context.getPackageManager();
        mParallelSpaceManager = ParallelSpaceManager.getInstance();
        mContext = context;
        loadSupportedAppData(userId);
    }

    void loadSupportedAppData(int userId) {
        final int dataVersion = ++mCurrentDataVersion;

        Thread thread = new Thread(() -> {
            List<SpaceAppInfo> apps = new ArrayList<>();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = mPackageManager.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA);
            List<ApplicationInfo> installedAppsOnSpace = mPackageManager.getInstalledApplicationsAsUser(0, userId);

            for (ResolveInfo info : infos) {
                if (info.activityInfo.applicationInfo.isSystemApp() ||
                        info.activityInfo.packageName.equals(mContext.getPackageName()))
                    continue;
                boolean isDuplicated = false;
                for (ApplicationInfo installedApp : installedAppsOnSpace) {
                    if (info.activityInfo.packageName.equals(installedApp.packageName)) {
                        isDuplicated = true;
                        break;
                    }
                }
                SpaceAppInfo app = new SpaceAppInfo(info, mPackageManager, mParallelSpaceManager, userId, isDuplicated, true);
                apps.add(app);
            }
            // Add GPlay and Files if app is enabled/ installed
            if (isAppEnabled(mContext, "com.android.vending")) {
                apps.add(new SpaceAppInfo(
                        getResolveInfoFor(mContext, "com.android.vending", "com.google.android.finsky.activities.MainActivity"),
                        mPackageManager, mParallelSpaceManager, userId, true, false));
            }
            if (isAppInstalled(mContext, "com.android.documentsui")) {
                apps.add(new SpaceAppInfo(
                        getResolveInfoFor(mContext, "com.android.documentsui", "com.android.documentsui.LauncherActivity"),
                        mPackageManager, mParallelSpaceManager, userId, true, false));
            }
            if (dataVersion == mCurrentDataVersion) {
                postValue(apps);
            }
        });
        thread.start();
    }
    private ResolveInfo getResolveInfoFor(Context context, String className, String activityName) {
        Intent intent = new Intent();
        intent.setClassName(className, activityName);
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo;
    }
}