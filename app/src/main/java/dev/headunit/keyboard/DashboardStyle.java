package dev.headunit.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import helium314.keyboard.latin.utils.KtxKt;

public final class DashboardStyle {
    private static final DashboardStyle[] CATALOG = {
        new DashboardStyle("Midnight", 0xff25394a, 0xff455e70, 0xff617e8f, 0xfff6fafc),
        new DashboardStyle("Porcelain", 0xffede5eb, 0xffd8cdd6, 0xffc9b9c8, 0xff332d35),
        new DashboardStyle("Graphite", 0xff232629, 0xff41464b, 0xff5c6267, 0xfffaf9f5)
    };
    public static final int RETRO_DRIVE = 3;
    public static final int THEME_COUNT = 4;

    public static DashboardStyle at(int index, boolean night) {
        if (index == RETRO_DRIVE && night) return new DashboardStyle("RetroDrive", 0xff222221, 0xff3b3b39, 0xff51514e, 0xfff3f2ec);
        if (index == RETRO_DRIVE) return new DashboardStyle("RetroDrive", 0xffeeeeec, 0xffefefec, 0xffc8c9c3, 0xff1d1d1c);
        return CATALOG[index];
    }

    public static final String SIZE_PERCENT = "dashboard_size_percent";
    public static final String NEVER_FULL_WIDTH = "dashboard_never_full_width";

    public static int floatingMargin(Context context) {
        return Math.round(8 * context.getResources().getDisplayMetrics().density);
    }

    public static float sizeScale(SharedPreferences prefs) {
        return Math.max(75, Math.min(120, prefs.getInt(SIZE_PERCENT, 100))) / 100f;
    }

    public static final int AMBER = 0xffffaf29;
    public final String name;
    public final int tray, face, number, ink;
    private static Typeface lettering;

    private DashboardStyle(String name, int tray, int face, int number, int ink) {
        this.name = name; this.tray = tray; this.face = face; this.number = number; this.ink = ink;
    }
    public static SharedPreferences prefs(Context context) { return KtxKt.prefs(context); }
    public static int currentIndex(Context context) {
        int selected = prefs(context).getInt("dashboard_theme", -1);
        if (selected >= 0 && selected < THEME_COUNT) return selected;
        return isNight(context) ? 2 : 1;
    }
    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }
    public static DashboardStyle current(Context context) { return at(currentIndex(context), isNight(context)); }
    public static String accentKey(int index) {
        return index == RETRO_DRIVE ? "dashboard_spec01_accent" : "dashboard_accent";
    }
    public static int accent(Context context, int index) {
        return prefs(context).getInt(accentKey(index), index == RETRO_DRIVE ? RetroDriveKeySurface.TEAL : AMBER);
    }
    public static int accent(Context context) { return accent(context, currentIndex(context)); }
    public static int contrast(int color) {
        double luminance = .2126 * linear(Color.red(color)) + .7152 * linear(Color.green(color)) + .0722 * linear(Color.blue(color));
        return luminance > .18 ? 0xff101518 : Color.WHITE;
    }
    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= .04045 ? c / 12.92 : Math.pow((c + .055) / 1.055, 2.4);
    }
    public static Typeface font(Context context) {
        if (lettering == null) lettering = Typeface.createFromAsset(context.getAssets(), "font/Barlow-SemiBold.ttf");
        return lettering;
    }
}
