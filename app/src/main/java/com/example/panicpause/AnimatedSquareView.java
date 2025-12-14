package com.example.panicpause;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;

public class AnimatedSquareView extends View {
    private Paint squarePaint;
    private Paint animatedPaint;
    private Path squarePath;
    private Path animatedPath;
    private PathMeasure pathMeasure;
    private float pathLength;
    private float animatedLength = 0;
    private float cornerRadius = 30f; // Должно совпадать с радиусом из XML
    private int squareColor = Color.parseColor("#CDC6A5"); // Цвет исходного квадрата
    private int animatedColor = Color.parseColor("#6F9283"); // Цвет анимации
    private float strokeWidth = 15f; // Толщина обводки

    public AnimatedSquareView(Context context) {
        super(context);
        init();
    }

    public AnimatedSquareView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedSquareView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Основной квадрат
        squarePaint = new Paint();
        squarePaint.setColor(squareColor);
        squarePaint.setStyle(Paint.Style.STROKE);
        squarePaint.setStrokeWidth(30);
        squarePaint.setAntiAlias(true);

        // Анимированный квадрат
        animatedPaint = new Paint();
        animatedPaint.setColor(animatedColor);
        animatedPaint.setStyle(Paint.Style.STROKE);
        animatedPaint.setStrokeCap(Paint.Cap.ROUND);
        animatedPaint.setStrokeWidth(30);
        animatedPaint.setAntiAlias(true);

        // Инициализация путей
        squarePath = new Path();
        animatedPath = new Path();
        pathMeasure = new PathMeasure();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        createSquarePath();
    }

    private void createSquarePath() {
        float width = getWidth();
        float height = getHeight();
        float padding = 15;

        // Начинаем с левого нижнего угла и идем по часовой стрелке
        squarePath.reset();
        squarePath.moveTo(padding , height - padding - cornerRadius ); // Левый нижний угол
        squarePath.lineTo(padding , padding + cornerRadius); // Вверх к левому верхнему углу
        squarePath.arcTo(padding, padding, padding + cornerRadius * 2, padding + cornerRadius * 2, 180, 90, false); // Скругление левого верхнего угла
        squarePath.lineTo(width - padding - cornerRadius, padding); // Вправо к правому верхнему углу
        squarePath.arcTo(width - padding - cornerRadius * 2, padding, width - padding, padding + cornerRadius * 2, 270, 90, false); // Скругление правого верхнего угла
        squarePath.lineTo(width - padding, height - padding - cornerRadius); // Вниз к правому нижнему углу
        squarePath.arcTo(width - padding - cornerRadius * 2, height - padding - cornerRadius * 2, width - padding, height - padding, 0, 90, false); // Скругление правого нижнего угла
        squarePath.lineTo(padding + cornerRadius, height - padding); // Влево к начальной точке
        squarePath.arcTo(padding, height - padding - cornerRadius * 2, padding + cornerRadius * 2, height - padding, 90, 90, false); // Замыкание
        squarePath.close();

        // Измеряем длину пути
        pathMeasure.setPath(squarePath, false);
        pathLength = pathMeasure.getLength();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Рисуем основной квадрат
        canvas.drawPath(squarePath, squarePaint);

        // Рисуем анимированный квадрат
        if (animatedLength > 0) {
            animatedPath.reset();
            pathMeasure.getSegment(0, animatedLength, animatedPath, true);
            canvas.drawPath(animatedPath, animatedPaint);
        }
    }

    // Устанавливает прогресс анимации (0.0 - 0%, 1.0 - 100%)
    public void setProgress(float progress) {
        animatedLength = progress * pathLength;
        invalidate(); // Перерисовка View
    }

    // Возвращает текущий прогресс анимации
    public float getProgress() {
        return animatedLength / pathLength;
    }
}




/*

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
*/
