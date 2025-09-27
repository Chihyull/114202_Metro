package com.example.a114202_metro.Favorite;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a114202_metro.R;

public class SwipteDeleteBar extends ItemTouchHelper.SimpleCallback {

    private final int revealWidthPx;
    private final RecyclerView.Adapter<?> adapter;

    // 只允許同時間一列展開
    private RecyclerView.ViewHolder openedVH = null;

    public SwipteDeleteBar(Context ctx, RecyclerView.Adapter<?> adapter, int revealWidthDp) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.revealWidthPx = Math.round(revealWidthDp * ctx.getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
        // 不使用系統的滑除
        int pos = vh.getAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) adapter.notifyItemChanged(pos);
    }

    // 關閉一列
    private void closeRow(RecyclerView.ViewHolder vh) {
        if (vh == null) return;
        View fg = vh.itemView.findViewById(R.id.foreground_container);
        View bar = vh.itemView.findViewById(R.id.btnDeleteBar);
        if (fg != null) fg.animate().translationX(0f).setDuration(120).start();
        if (bar != null) bar.setVisibility(View.INVISIBLE);
    }

    // 展開一列
    private void openRow(RecyclerView.ViewHolder vh) {
        if (vh == null) return;
        View fg = vh.itemView.findViewById(R.id.foreground_container);
        View bar = vh.itemView.findViewById(R.id.btnDeleteBar);
        if (fg != null) fg.animate().translationX(-revealWidthPx).setDuration(120).start();
        if (bar != null) bar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c,
                            @NonNull RecyclerView rv,
                            @NonNull RecyclerView.ViewHolder vh,
                            float dX, float dY,
                            int actionState,
                            boolean isCurrentlyActive) {

        View foreground = vh.itemView.findViewById(R.id.foreground_container);
        View deleteBar  = vh.itemView.findViewById(R.id.btnDeleteBar);
        if (foreground == null || deleteBar == null) return;

        float currentTx = foreground.getTranslationX();

        if (isCurrentlyActive) {
            // 如果有別列已展開，且現在在滑另一列 → 先關掉舊的
            if (openedVH != null && openedVH != vh) {
                closeRow(openedVH);
                openedVH = null;
            }

            if (dX < 0) {
                // 左滑：跟手，最多到 -revealWidth
                float clamped = Math.max(dX, -revealWidthPx);
                foreground.setTranslationX(clamped);
                if (deleteBar.getVisibility() != View.VISIBLE) deleteBar.setVisibility(View.VISIBLE);
            } else if (dX > 0) {
                // 右滑：如果本列已展開，允許往回推到 0；未展開則維持 0
                float base = Math.min(currentTx, 0f); // currentTx 不是負的就當 0
                float clamped = Math.min(base + dX, 0f);
                foreground.setTranslationX(clamped);
                if (clamped >= 0f && deleteBar.getVisibility() != View.INVISIBLE) {
                    deleteBar.setVisibility(View.INVISIBLE);
                }
            }
            // dX==0 不動

        } else {
            // 放手：任何「左滑到過負值」都直接吸附到展開；沒有左滑/被右推回去就收回
            float tx = foreground.getTranslationX();
            if (tx < 0f) {
                // ✅ 不再需要超過一半；只要有左滑就展開停駐
                openRow(vh);
                openedVH = vh;
            } else {
                closeRow(vh);
                if (openedVH == vh) openedVH = null;
            }
        }

        // 不呼叫 super，避免預設 remove 動畫
        // super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
        // 不強制復位，保持當前狀態（展開或關閉）
        super.clearView(rv, vh);
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 1.0f;
    }
}





