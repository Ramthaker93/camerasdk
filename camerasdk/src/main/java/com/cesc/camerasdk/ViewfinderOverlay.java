package com.cesc.camerasdk;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ViewfinderOverlay extends View {
    private final Paint maskPaint = new Paint();
    private final Paint holePaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final RectF holeRect = new RectF();

    public ViewfinderOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        maskPaint.setColor(Color.BLACK); // Fully opaque black
        maskPaint.setStyle(Paint.Style.FILL);

        holePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        holePaint.setAntiAlias(true);

        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);
        borderPaint.setAntiAlias(true);

        // Required to make PorterDuff.Mode.CLEAR work
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Define the rectangular hole in the middle
        float width = w * 0.8f;  // 80% of screen width
        float height = width * 0.5f; // Rectangle ratio
        float left = (w - width) / 2;
        float top = (h - height) / 3; // Perfectly centered vertically
        holeRect.set(left, top, left + width, top + height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Draw the semi-transparent mask
        canvas.drawRect(0, 0, getWidth(), getHeight(), maskPaint);

        // Clear the rectangular hole
        canvas.drawRect(holeRect, holePaint);

        // Draw the border around the hole
        canvas.drawRect(holeRect, borderPaint);
    }
}

