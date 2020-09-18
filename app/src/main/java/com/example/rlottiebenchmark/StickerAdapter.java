package com.example.rlottiebenchmark;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rlottiebenchmark.widget.RLottieDrawable;
import com.example.rlottiebenchmark.widget.RLottieImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.StickerHolder> {

    private List<StickerUiModel> stickers = new ArrayList<>();

    private ExecutorService mExecutor = Executors.newFixedThreadPool(5);

    @NonNull
    @Override
    public StickerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = new RLottieImageView(parent.getContext());
        return new StickerHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StickerHolder holder, int position) {
        holder.bindView(stickers.get(position), mExecutor);
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

        private Future currentTask;

        public StickerHolder(@NonNull View itemView) {
            super(itemView);
            mImgView = (RLottieImageView) itemView;
        }

        public void bindView(final StickerUiModel stickerUiModel, ExecutorService executor) {
            mImgView.invalidate();
            if (currentTask != null && (!currentTask.isCancelled() || !currentTask.isDone())) {
                currentTask.cancel(true);
            }
            currentTask = executor.submit(() -> {
                RLottieDrawable drawable = new RLottieDrawable(new File(stickerUiModel.path), AndroidUtilities.dp(stickerUiModel.width), AndroidUtilities.dp(stickerUiModel.height), true, false);
                AndroidUtilities.runOnUIThread(() -> {
                    mImgView.setAutoRepeat(true);
                    mImgView.setAnimation(drawable);
                    mImgView.playAnimation();
                });
            });

        }
    }

    public static class StickerUiModel {
        private final String path;

        private final int width;

        private final int height;

        public StickerUiModel(String path, int width, int height) {
            this.path = path;
            this.width = width;
            this.height = height;
        }
    }
}
