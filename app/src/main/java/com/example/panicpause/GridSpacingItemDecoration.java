package com.example.panicpause;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Класс GridSpacingItemDecoration предназначен для оформления списка избранных упражнений.
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacing;
    public GridSpacingItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int column = position % 2; // 0 = левый, 1 = правый

        outRect.left = column * spacing / 2;
        outRect.right = spacing - (column * spacing / 2);
        outRect.bottom = spacing;
        outRect.top = 0;
    }
}
