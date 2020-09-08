package com.example.rlottiebenchmark;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rlottiebenchmark.widget.RLottieDrawable;
import com.example.rlottiebenchmark.widget.RLottieImageView;

import java.util.ArrayList;
import java.util.List;

class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.StickerHolder> {

    private List<StickerUiModel> stickers = new ArrayList<>();

    private DispatchQueuePool mDispatchQueuePool = new DispatchQueuePool(4);

    @NonNull
    @Override

    public StickerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = new RLottieImageView(parent.getContext());
        return new StickerHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StickerHolder holder, int position) {
        holder.bindView(stickers.get(position), mDispatchQueuePool);
    }

    @Override
    public int getItemCount() {
        return stickers.size();
    }

    public void setStickers(final List<StickerUiModel> stickers) {
        this.stickers.clear();
        this.stickers.addAll(stickers);
        notifyDataSetChanged();
    }

    static class StickerHolder extends RecyclerView.ViewHolder {

        private RLottieImageView mImgView;

        public StickerHolder(@NonNull View itemView) {
            super(itemView);
            mImgView = (RLottieImageView) itemView;
        }

        public void bindView(final StickerUiModel stickerUiModel, DispatchQueuePool dispatchQueuePool) {
            dispatchQueuePool.execute(() -> {
                RLottieDrawable drawable = new RLottieDrawable(stickerUiModel.resId, "" + stickerUiModel.resId, AndroidUtilities.dp(stickerUiModel.width), AndroidUtilities.dp(stickerUiModel.height), false, null);
                AndroidUtilities.runOnUIThread(() -> {
                    mImgView.setAutoRepeat(true);
                    mImgView.setAnimation(drawable);
                    mImgView.playAnimation();
                });
            });
        }
    }

    public static class StickerUiModel {
        @IdRes
        private final int resId;

        private final int width;

        private final int height;

        public StickerUiModel(int resId, int width, int height) {
            this.resId = resId;
            this.width = width;
            this.height = height;
        }
    }
}
