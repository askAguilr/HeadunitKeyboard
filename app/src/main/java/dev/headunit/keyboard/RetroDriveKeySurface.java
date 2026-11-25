package dev.headunit.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import java.io.IOException;

/** Molded key surfaces adapted from RetroDrive's native hardware buttons. */
public final class RetroDriveKeySurface {
    public static final int TEAL = 0xff216d73;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outline = new Path();
    private final Shader rim = new LinearGradient(-.28f, -.5f, .24f, .46f,
            new int[]{0xeaffffff, 0x55ffffff, 0x05ffffff, 0x38ffffff},
            new float[]{0f, .33f, .68f, 1f}, Shader.TileMode.CLAMP);
    private final int accent;
    private final int rimAlpha;
    private final int unlitIndicator;
    private final Shader indicatorGlow;

    private static final class Material {
        final Shader face;
        final int wall, lowerEdge;

        Material(int top, int mid, int bottom, int wall, int lowerEdge) {
            this.face = gradient(top, mid, bottom);
            this.wall = wall;
            this.lowerEdge = lowerEdge;
        }
    }

    private final Material letter;
    private final Material lcd;
    private final Material primary;

    public RetroDriveKeySurface(int accent, boolean night) {
        letter = night ? new Material(0xff434341, 0xff3b3b39, 0xff464643, 0xff30302e, 0xff171716)
                : new Material(0xffecece9, 0xffefefec, 0xfffdfdfc, 0xffd2d2cc, 0xffaaa9a3);
        lcd = night ? new Material(0xff555552, 0xff51514e, 0xff5a5a57, 0xff393937, 0xff262624)
                : new Material(0xffb6b7b1, 0xffc8c9c3, 0xffc2c3bd,
                        tint(0xffc8c9c3, .83f), tint(0xffc8c9c3, .69f));

        this.accent = accent;
        unlitIndicator = night ? 0xff565652 : 0xffcfcdc7;
        rimAlpha = night ? 78 : 255;
        int rgb = accent & 0x00ffffff;
        indicatorGlow = new RadialGradient(0, 0, .15f,
                new int[]{accent, 0x80000000 | rgb, 0x26000000 | rgb, rgb},
                new float[]{0f, .30f, .66f, 1f}, Shader.TileMode.CLAMP);
        primary = new Material(tint(accent, .90f), accent, tint(accent, 1.12f),
                tint(accent, .78f), tint(accent, .62f));
    }

    public static Typeface loadFont(Context context) {
        // Build a view-owned font instead of entering the process-wide asset typeface cache.
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                android.graphics.fonts.Font font = new android.graphics.fonts.Font.Builder(
                        context.getAssets(), "retrodrive/SpecMono-Medium.ttf").build();
                return new Typeface.CustomFallbackBuilder(new android.graphics.fonts.FontFamily.Builder(font).build())
                        .setSystemFallback("monospace").build();
            } catch (IOException ignored) { }
        }
        return Typeface.MONOSPACE;
    }

    private static LinearGradient gradient(int top, int mid, int bottom) {
        return new LinearGradient(0, -.5f, 0, .5f, new int[]{top, mid, bottom},
                new float[]{0f, .3f, 1f}, Shader.TileMode.CLAMP);
    }

    private static int tint(int color, float factor) {
        return Color.rgb(Math.min(255, Math.round(Color.red(color) * factor)),
                Math.min(255, Math.round(Color.green(color) * factor)),
                Math.min(255, Math.round(Color.blue(color) * factor)));
    }

    // Broad shoulders ease into the flat sides. A circular arc starts turning too abruptly.
    private void cap(Canvas c, float l, float t, float r, float b, boolean round) {
        float height = b - t;
        if (round) {
            c.drawRoundRect(l, t, r, b, height / 2, height / 2, paint);
            return;
        }
        float bend = Math.min(r - l, height) * .34f;
        float near = bend * .22f;
        outline.rewind();
        outline.moveTo(l + bend, t);
        outline.lineTo(r - bend, t);
        outline.cubicTo(r - near, t, r, t + near, r, t + bend);
        outline.lineTo(r, b - bend);
        outline.cubicTo(r, b - near, r - near, b, r - bend, b);
        outline.lineTo(l + bend, b);
        outline.cubicTo(l + near, b, l, b - near, l, b - bend);
        outline.lineTo(l, t + bend);
        outline.cubicTo(l, t + near, l + near, t, l + bend, t);
        outline.close();
        c.drawPath(outline, paint);
    }

    public void draw(Canvas c, float cx, float cy, float w, float h,
                     boolean number, boolean action, float pressed, boolean shiftKey, boolean shiftLit) {
        if (h <= 0 || w <= 0) return;
        c.save();
        c.translate(cx, cy);
        c.scale(h, h);
        float half = w / (2 * h);
        float travel = .035f * pressed;
        Material material = action ? primary : number ? lcd : letter;
        paint.setShader(null);
        paint.setColor(0x12000000);
        cap(c, -half, -.43f, half, .50f, number);
        paint.setColor(material.lowerEdge);
        cap(c, -half, -.44f, half, .483f, number);
        paint.setColor(material.wall);
        cap(c, -half, -.49f, half, .474f, number);
        paint.setColor(Color.WHITE);
        paint.setShader(material.face);
        cap(c, -half + .009f, -.5f + travel, half - .009f, .436f + travel, number);
        paint.setShader(rim);
        paint.setAlpha(rimAlpha);
        cap(c, -half + .009f, -.5f + travel, half - .009f, .436f + travel, number);
        paint.setAlpha(255);
        paint.setShader(material.face);
        cap(c, -half + .020f, -.479f + travel, half - .020f, .429f + travel, number);
        paint.setShader(null);
        if (pressed > 0) {
            paint.setColor(Color.argb(Math.round(14 * pressed), 0, 0, 0));
            cap(c, -half + .009f, -.5f + travel, half - .009f, .436f + travel, number);
        }
        if (shiftKey) {
            c.translate(half - .20f, -.31f + travel);
            if (shiftLit) {
                paint.setColor(Color.WHITE);
                paint.setShader(indicatorGlow);
                c.drawCircle(0, 0, .15f, paint);
                paint.setShader(null);
            }
            paint.setColor(shiftLit ? accent : unlitIndicator);
            c.drawCircle(0, 0, .052f, paint);
        }
        c.restore();
    }
}
