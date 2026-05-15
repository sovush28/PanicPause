package com.example.panicpause;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;

/**
 * Класс AnimatedSquareView рисует и анимирует квадрат для упражнения "Дыхание по квдрату".
 */
public class AnimatedSquareView extends View {
    private Paint squarePaint;
    private Paint animatedPaint;
    private Path squarePath;
    private Path animatedPath;
    private PathMeasure pathMeasure;
    private float pathLength;
    private float animatedLength = 0;
    private float cornerRadius = 30f; // должно совпадать с радиусом из XML
    private int squareColor = Color.parseColor("#CDC6A5"); // цвет исходного квадрата
    private int animatedColor = Color.parseColor("#6F9283"); // цвет анимации
    private float strokeWidth = 15f; // толщина обводки

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
        // основной квадрат
        squarePaint = new Paint();
        squarePaint.setColor(squareColor);
        squarePaint.setStyle(Paint.Style.STROKE);
        squarePaint.setStrokeWidth(30);
        squarePaint.setAntiAlias(true);

        // анимированный квадрат
        animatedPaint = new Paint();
        animatedPaint.setColor(animatedColor);
        animatedPaint.setStyle(Paint.Style.STROKE);
        animatedPaint.setStrokeCap(Paint.Cap.ROUND);
        animatedPaint.setStrokeWidth(30);
        animatedPaint.setAntiAlias(true);

        // инициализация путей
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

        // начинает с левого нижнего угла и идет по часовой стрелке
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

        // измерение длины пути
        pathMeasure.setPath(squarePath, false);
        pathLength = pathMeasure.getLength();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // нарисовать основной квадрат
        canvas.drawPath(squarePath, squarePaint);

        // нарисовать анимированный квадрат
        if (animatedLength > 0) {
            animatedPath.reset();
            pathMeasure.getSegment(0, animatedLength, animatedPath, true);
            canvas.drawPath(animatedPath, animatedPaint);
        }
    }

    /**
     * Метод setProgress устанавливает прогресс анимации (0.0 - 0%, 1.0 - 100%)
     */
    public void setProgress(float progress) {
        animatedLength = progress * pathLength;
        invalidate(); // Перерисовка View
    }

    /**
     * Метод getProgress возвращает текущий прогресс анимации
     * @return рогресс анимации (0.0 - 0%, 1.0 - 100%)
     */
    public float getProgress() {
        return animatedLength / pathLength;
    }
}
