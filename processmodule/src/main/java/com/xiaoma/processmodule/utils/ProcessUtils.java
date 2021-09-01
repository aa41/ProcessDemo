package com.xiaoma.processmodule.utils;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.List;


public class ProcessUtils {
    private ProcessUtils() {
    }

    public static Boolean isMainProcess(Context context) {
        if (context == null) {
            return null;
        }
        String mainProcessName = null;
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        if (applicationInfo != null) {
            mainProcessName = applicationInfo.processName;
        }
        if (TextUtils.isEmpty(mainProcessName)) {
            mainProcessName = context.getPackageName();
        }
        String processName = getProcessName(context);
        if (TextUtils.isEmpty(mainProcessName) || TextUtils.isEmpty(processName)) {
            return null;
        }
        return mainProcessName.equals(processName);
    }


    @Nullable
    @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
    public static String getProcessName(@NonNull Context context) {
        if (SDK_INT >= 28) {
            return Application.getProcessName();
        }
        // Try using ActivityThread to determine the current process name.
        try {
            Class<?> activityThread = Class.forName(
                    "android.app.ActivityThread",
                    false,
                    ProcessUtils.class.getClassLoader());
            final Object packageName;
            if (SDK_INT >= 18) {
                Method currentProcessName = activityThread.getDeclaredMethod("currentProcessName");
                currentProcessName.setAccessible(true);
                packageName = currentProcessName.invoke(null);
            } else {
                Method getActivityThread = activityThread.getDeclaredMethod(
                        "currentActivityThread");
                getActivityThread.setAccessible(true);
                Method getProcessName = activityThread.getDeclaredMethod("getProcessName");
                getProcessName.setAccessible(true);
                packageName = getProcessName.invoke(getActivityThread.invoke(null));
            }
            if (packageName instanceof String) {
                return (String) packageName;
            }
        } catch (Throwable exception) {
            exception.printStackTrace();
        }
        int pid = Process.myPid();
        ActivityManager am =
                (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null && !processes.isEmpty()) {
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (process.pid == pid) {
                        return process.processName;
                    }
                }
            }
        }
        return null;
    }
}
