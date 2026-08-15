package com.mycompany.application;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public final class WindowIconDrawable extends Drawable {

    public static final int REFRESH = 1;
    public static final int BACK = 2;
    public static final int FORWARD = 3;
    public static final int MINIMIZE = 4;
    public static final int MAXIMIZE = 5;
    public static final int CLOSE = 6;
    public static final int RESIZE = 7;
    public static final int APP_MARK = 8;
    public static final int ZOOM_OUT = 9;
    public static final int ZOOM_IN = 10;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final int type;
    private final int color;

    public WindowIconDrawable(int type, int color) {
        this.type = type;
        this.color = color;
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeJoin(Paint.Join.MITER);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float cx = bounds.exactCenterX();
        float cy = bounds.exactCenterY();
        float size = Math.min(bounds.width(), bounds.height()) * 0.48f;
        float stroke = Math.max(1.4f, size * 0.105f);

        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setShader(null);

        switch (type) {
            case REFRESH:
                rect.set(cx - size * 0.58f, cy - size * 0.58f,
                        cx + size * 0.58f, cy + size * 0.58f);
                canvas.drawArc(rect, 38.0f, 278.0f, false, paint);
                path.reset();
                path.moveTo(cx + size * 0.48f, cy - size * 0.52f);
                path.lineTo(cx + size * 0.62f, cy - size * 0.08f);
                path.lineTo(cx + size * 0.20f, cy - size * 0.16f);
                canvas.drawPath(path, paint);
                break;
            case BACK:
                path.reset();
                path.moveTo(cx + size * 0.52f, cy - size * 0.52f);
                path.lineTo(cx - size * 0.40f, cy);
                path.lineTo(cx + size * 0.52f, cy + size * 0.52f);
                canvas.drawPath(path, paint);
                break;
            case FORWARD:
                path.reset();
                path.moveTo(cx - size * 0.52f, cy - size * 0.52f);
                path.lineTo(cx + size * 0.40f, cy);
                path.lineTo(cx - size * 0.52f, cy + size * 0.52f);
                canvas.drawPath(path, paint);
                break;
            case MINIMIZE:
                canvas.drawLine(cx - size * 0.58f, cy + size * 0.30f,
                        cx + size * 0.58f, cy + size * 0.30f, paint);
                break;
            case MAXIMIZE:
                rect.set(cx - size * 0.50f, cy - size * 0.50f,
                        cx + size * 0.50f, cy + size * 0.50f);
                canvas.drawRect(rect, paint);
                break;
            case CLOSE:
                canvas.drawLine(cx - size * 0.48f, cy - size * 0.48f,
                        cx + size * 0.48f, cy + size * 0.48f, paint);
                canvas.drawLine(cx + size * 0.48f, cy - size * 0.48f,
                        cx - size * 0.48f, cy + size * 0.48f, paint);
                break;
            case RESIZE:
                canvas.drawLine(cx + size * 0.58f, cy - size * 0.58f,
                        cx + size * 0.58f, cy + size * 0.58f, paint);
                canvas.drawLine(cx - size * 0.58f, cy + size * 0.58f,
                        cx + size * 0.58f, cy + size * 0.58f, paint);
                canvas.drawLine(cx + size * 0.12f, cy + size * 0.58f,
                        cx + size * 0.58f, cy + size * 0.12f, paint);
                break;
            case APP_MARK: {
                paint.setStyle(Paint.Style.FILL);
                float gap = size * 0.10f;
                float tile = size * 0.38f;
                canvas.drawRect(cx - tile - gap, cy - tile, cx - gap, cy - gap, paint);
                canvas.drawRect(cx + gap, cy - tile, cx + tile + gap, cy - gap, paint);
                canvas.drawRect(cx - tile - gap, cy + gap, cx - gap, cy + tile, paint);
                canvas.drawRect(cx + gap, cy + gap, cx + tile + gap, cy + tile, paint);
                break;
            }
            case ZOOM_OUT:
                canvas.drawCircle(cx - size * 0.12f, cy - size * 0.12f,
                        size * 0.43f, paint);
                canvas.drawLine(cx + size * 0.20f, cy + size * 0.20f,
                        cx + size * 0.58f, cy + size * 0.58f, paint);
                canvas.drawLine(cx - size * 0.34f, cy - size * 0.12f,
                        cx + size * 0.10f, cy - size * 0.12f, paint);
                break;
            case ZOOM_IN:
                canvas.drawCircle(cx - size * 0.12f, cy - size * 0.12f,
                        size * 0.43f, paint);
                canvas.drawLine(cx + size * 0.20f, cy + size * 0.20f,
                        cx + size * 0.58f, cy + size * 0.58f, paint);
                canvas.drawLine(cx - size * 0.34f, cy - size * 0.12f,
                        cx + size * 0.10f, cy - size * 0.12f, paint);
                canvas.drawLine(cx - size * 0.12f, cy - size * 0.34f,
                        cx - size * 0.12f, cy + size * 0.10f, paint);
                break;
            default:
                break;
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}