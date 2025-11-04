package com.example.safespace;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class AnimatedSquareView extends View {
    private Paint strokePaint;
    private Paint fillPaint;
    private Path squarePath;
    private float strokeWidth = 15f;
    private float lengthAnimated = 0f; // Length of the stroke animated so far

    public AnimatedSquareView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokePaint = new Paint();
        strokePaint.setColor(getResources().getColor(R.color.colorLightGreen));  // Initial stroke color
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setAntiAlias(true);

        fillPaint = new Paint();
        fillPaint.setColor(getResources().getColor(R.color.colorSoftGreen)); // Fill color
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        squarePath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the filled square
        canvas.drawRect(0, 0, getWidth(), getHeight(), fillPaint);

        // Draw the path for the stroke
        squarePath.reset();
        // Move to the bottom-left corner
        squarePath.moveTo(0, getHeight());

        // Draw the current stroke length based on how much has animated
        if (lengthAnimated <= getWidth()) {
            squarePath.lineTo(lengthAnimated, getHeight()); // Bottom edge
        } else if (lengthAnimated <= getWidth() + getHeight()) {
            squarePath.lineTo(getWidth(), getHeight() - (lengthAnimated - getWidth())); // Left edge
        } else if (lengthAnimated <= 2 * getWidth() + getHeight()) {
            squarePath.lineTo(getWidth() - (lengthAnimated - (getWidth() + getHeight())), 0); // Top edge
        } else if (lengthAnimated <= 3 * getWidth() + getHeight()) {
            squarePath.lineTo(0, (lengthAnimated - (2 * getWidth() + getHeight()))); // Right edge
        } else {
            squarePath.lineTo(0, getHeight()); // Close to bottom-left corner again
        }

        canvas.drawPath(squarePath, strokePaint);
    }

    public void animateStroke(float length) {
        this.lengthAnimated = length;
        invalidate(); // Request to redraw the view
    }
}
