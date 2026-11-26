package com.accurateaccounts.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskPreferenceManager {

    private static final String PREF_NAME = "aas_task_seen_prefs";
    private static final String KEY_SEEN_PREFIX = "seen_task_ids_";

    public static Set<String> getSeenTaskIds(Context context, int userId) {
        if (context == null) return new HashSet<>();
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(sp.getStringSet(KEY_SEEN_PREFIX + userId, new HashSet<>()));
    }

    public static boolean isTaskNew(Context context, int userId, int taskId) {
        Set<String> seen = getSeenTaskIds(context, userId);
        return !seen.contains(String.valueOf(taskId));
    }

    public static void markTaskAsSeen(Context context, int userId, int taskId) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> seen = new HashSet<>(sp.getStringSet(KEY_SEEN_PREFIX + userId, new HashSet<>()));
        seen.add(String.valueOf(taskId));
        sp.edit().putStringSet(KEY_SEEN_PREFIX + userId, seen).apply();
    }

    public static void markAllTasksAsSeen(Context context, int userId, List<Integer> taskIds) {
        if (context == null || taskIds == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> seen = new HashSet<>(sp.getStringSet(KEY_SEEN_PREFIX + userId, new HashSet<>()));
        for (Integer id : taskIds) {
            seen.add(String.valueOf(id));
        }
        sp.edit().putStringSet(KEY_SEEN_PREFIX + userId, seen).apply();
    }
}
