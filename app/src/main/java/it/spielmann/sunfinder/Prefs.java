package it.spielmann.sunfinder;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {

    private static final String PREFS_NAME = "sunfinder_prefs";
    private static final String KEY_PREFER_SHADE = "prefer_shade";

    private Prefs() {}

    static boolean isPreferShade(Context context) {
        return prefs(context).getBoolean(KEY_PREFER_SHADE, true);
    }

    static void setPreferShade(Context context, boolean preferShade) {
        prefs(context).edit().putBoolean(KEY_PREFER_SHADE, preferShade).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
