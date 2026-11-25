package helium314.keyboard.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.inputmethod.EditorInfo;
import dev.headunit.keyboard.DashboardStyle;
import dev.headunit.keyboard.KeycapShape;
import dev.headunit.keyboard.RetroDriveKeySurface;
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode;

final class DashboardCaps {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private Typeface font;
    private RetroDriveKeySurface surface;
    private int theme = -1;
    private final Context context;
    private DashboardStyle style;
    private int accent;
    private boolean night;
    DashboardCaps(Context context) {
        this.context = context;
        refresh();
    }
    void refresh() {
        int index = DashboardStyle.currentIndex(context);
        int nextAccent = DashboardStyle.accent(context, index);
        boolean nextNight = DashboardStyle.isNight(context);
        if (index == theme && accent == nextAccent && night == nextNight) return;
        releaseSurface();
        theme = index;
        night = nextNight;
        accent = nextAccent;
    }
    void releaseSurface() {
        surface = null;
        font = null;
        paint.setTypeface(null);
        style = null;
    }
    int background() { return DashboardStyle.at(theme, night).tray; }
    boolean usesRetroDriveSurface() { return theme == DashboardStyle.RETRO_DRIVE; }

    private void line(Canvas canvas, float... xy) {
        path.rewind();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i+1]);
        canvas.drawPath(path, paint);
    }

    void draw(Canvas c, Key key, Keyboard board, float touch) {
        if (key.isSpacer()) return;
        float w = key.getDrawWidth(), h = key.getHeight(), cx = w/2, cy = h/2;
        int code = key.getCode();
        boolean digit = Character.isDigit(code);
        boolean zero = digit && Character.digit(code, 10) == 0;
        boolean padDecimal = code == '.' && board.mId.isAlphaOrSymbolKeyboard()
                && key.getX() > board.mId.mWidth * .90f
                && context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        boolean action = key.hasActionKeyBackground() || code == 10;
        boolean wide = action || code == ' ' || code == KeyCode.DELETE || code == KeyCode.SHIFT;
        KeycapShape shape;
        if (zero) shape = KeycapShape.PILL;
        else if (digit || padDecimal) shape = KeycapShape.CIRCLE;
        else shape = wide ? KeycapShape.WIDE : KeycapShape.SQUARE;
        // Layout has already subtracted the same physical gap from each cell.
        float faceHeight = Math.min(w, h);
        float faceWidth = shape == KeycapShape.WIDE || shape == KeycapShape.PILL ? w : faceHeight;
        float radius = shape.radius(faceHeight);
        c.save();
        if (style == null) style = DashboardStyle.at(theme, night);
        if (font == null) font = theme == DashboardStyle.RETRO_DRIVE
                ? RetroDriveKeySurface.loadFont(context) : DashboardStyle.font(context);
        if (theme == DashboardStyle.RETRO_DRIVE && surface == null) surface = new RetroDriveKeySurface(accent, night);
        if (surface == null) c.scale(1 - .07f*touch, 1 - .07f*touch, cx, cy);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(action ? accent : digit || padDecimal ? style.number : style.face);
        if (surface != null) {
            surface.draw(c, cx, cy, faceWidth, faceHeight, digit || padDecimal, action, touch,
                    code == KeyCode.SHIFT && board.mId.isAlphabetKeyboard(), board.mId.isAlphabetShifted());
            cy += faceHeight * (.035f * touch - .024f);
        } else if (shape == KeycapShape.CIRCLE) {
            c.drawCircle(cx, cy, radius, paint);
        } else {
            c.drawRoundRect(cx - faceWidth/2, cy - faceHeight/2, cx + faceWidth/2,
                    cy + faceHeight/2, radius, radius, paint);
        }
        int ink = action ? DashboardStyle.contrast(accent) : style.ink;
        paint.setColor(ink);
        boolean symbol = code == KeyCode.DELETE || (code == KeyCode.SHIFT && board.mId.isAlphabetKeyboard()) || code == KeyCode.IME_HIDE_UI;
        if (symbol) {
            c.translate(cx,cy);
            float scale = faceHeight * .020f;
            c.scale(scale,scale);
            paint.setStrokeWidth(1.65f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            if (code == KeyCode.DELETE) {
                line(c,-11,0,-5,-7.5f,10,-7.5f,10,7.5f,-5,7.5f,-11,0);
                line(c,-1,-3,5,3); line(c,5,-3,-1,3);
            } else if (code == KeyCode.IME_HIDE_UI) {
                line(c,-7,-3,0,4,7,-3);
            } else {
                line(c,-8,0,0,-8,8,0,4,0,4,6,-4,6,-4,0,-8,0);
                if (board.mId.mElementId > 0) line(c,-4,9,4,9);
            }
        } else if (code != ' ') {
            String label = key.getLabel();
            if (action && board.mId.mInternalAction == null) {
                label = board.mId.mCustomActionLabel;
                if (label == null) label = switch (board.mId.imeAction()) {
                    case EditorInfo.IME_ACTION_SEARCH -> "Search";
                    case EditorInfo.IME_ACTION_GO -> "Go";
                    case EditorInfo.IME_ACTION_NEXT -> "Next";
                    case EditorInfo.IME_ACTION_SEND -> "Send";
                    case EditorInfo.IME_ACTION_DONE -> "Done";
                    case EditorInfo.IME_ACTION_PREVIOUS -> "Prev";
                    default -> "â†µ";
                };
            }
            if (label != null) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTypeface(font);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(faceHeight * .41f);
                float width = paint.measureText(label);
                if (width > faceWidth*.85f) paint.setTextSize(paint.getTextSize() * faceWidth*.85f/width);
                String hint = board.mId.mElementId == KeyboardId.ELEMENT_PHONE && digit ? key.getHintLabel() : null;
                float labelY = hint == null ? cy : cy - faceHeight * .10f;
                c.drawText(label,cx,labelY-(paint.ascent()+paint.descent())/2,paint);
                if (hint != null) {
                    paint.setTextSize(faceHeight * .16f);
                    c.drawText(hint,cx,cy + faceHeight * .27f,paint);
                }
            } else {
                Drawable icon = key.getIcon(board.mIconsSet,255);
                if (icon != null) {
                    int size = (int)(faceHeight*.49f);
                    icon.setTint(ink);
                    icon.setBounds((int)cx-size/2,(int)cy-size/2,(int)cx+size/2,(int)cy+size/2);
                    icon.draw(c);
                }
            }
        }
        c.restore();
    }
}
