package com.mycompany.application;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public final class LiquidGlassDrawable {

    private LiquidGlassDrawable() {
    }

    public static final class MenuIcon extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float width = bounds.width();
            float height = bounds.height();
            float centerX = bounds.exactCenterX();
            float centerY = bounds.exactCenterY();
            float radius = Math.max(1.0f, Math.min(width, height) * 0.46f);

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(
                    centerX - radius * 0.22f,
                    centerY - radius * 0.24f,
                    radius * 1.55f,
                    new int[]{0x663CCBFF, 0x30227FBD, 0x00205B92},
                    null,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(centerX, centerY, radius * 1.22f, paint);

            paint.setShader(new LinearGradient(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    new int[]{0xD9DDF8FF, 0x9B75C9E9, 0xBC31688F, 0xD91A294C},
                    null,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(centerX, centerY, radius, paint);

            canvas.save();
            Path clip = new Path();
            clip.addCircle(centerX, centerY, radius, Path.Direction.CW);
            canvas.clipPath(clip);

            paint.setShader(new RadialGradient(
                    centerX - radius * 0.42f,
                    centerY - radius * 0.52f,
                    radius * 0.92f,
                    new int[]{0xB8ECFBFF, 0x4A7AD5F2, 0x002A8ED0},
                    null,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(centerX - radius * 0.26f, centerY - radius * 0.27f, radius, paint);

            paint.setShader(new RadialGradient(
                    centerX + radius * 0.46f,
                    centerY + radius * 0.34f,
                    radius * 0.88f,
                    new int[]{0x5270DFFF, 0x222C77CB, 0x001D477B},
                    null,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(centerX + radius * 0.24f, centerY + radius * 0.22f, radius, paint);

            paint.setShader(new LinearGradient(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius,
                    new int[]{0x80FFFFFF, 0x08FFFFFF, 0x00000000},
                    null,
                    Shader.TileMode.CLAMP));
            oval.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            canvas.drawArc(oval, 198.0f, 108.0f, false, highlightPaint());
            canvas.restore();

            paint.setShader(new LinearGradient(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    new int[]{0xE6F5FDFF, 0x9A7DD6F4, 0xA83E8CC1, 0xC4BDEBFF},
                    null,
                    Shader.TileMode.CLAMP));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.0f, radius * 0.065f));
            canvas.drawCircle(centerX, centerY, radius - paint.getStrokeWidth(), paint);
        }

        private Paint highlightPaint() {
            Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
            highlight.setStyle(Paint.Style.STROKE);
            highlight.setStrokeWidth(1.0f);
            highlight.setShader(paint.getShader());
            return highlight;
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

    public static final class Panel extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float inset = Math.max(1.0f, Math.min(bounds.width(), bounds.height()) * 0.025f);
            float radius = Math.min(bounds.height() * 0.34f, 16.0f);
            rect.set(bounds.left + inset, bounds.top + inset,
                    bounds.right - inset, bounds.bottom - inset);

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom,
                    new int[]{0xD93B6382, 0xC91D3757, 0xD916243D},
                    null,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, paint);

            paint.setShader(new RadialGradient(
                    rect.left + rect.width() * 0.18f,
                    rect.top + rect.height() * 0.05f,
                    rect.width() * 0.72f,
                    new int[]{0x5ED7F6FF, 0x142F9ED2, 0x00000000},
                    null,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.0f, inset));
            paint.setShader(new LinearGradient(
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom,
                    new int[]{0xC8DDFBFF, 0x5278D3EC, 0x7B75A9E4},
                    null,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, paint);
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
}