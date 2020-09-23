package com.example.rlottiebenchmark.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.example.rlottiebenchmark.AndroidUtilities;

import java.util.HashMap;

public class RLottieImageView extends AppCompatImageView {

    private RLottieDrawable drawable;
    private boolean autoRepeat;
    private boolean attachedToWindow;
    private boolean playing;

    public RLottieImageView(Context context) {
        super(context);
    }

    public RLottieImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setAnimation(RLottieDrawable lottieDrawable) {
        drawable = lottieDrawable;
        if (autoRepeat) {
            drawable.setAutoRepeat(1);
        }

        drawable.setAllowDecodeSingleFrame(true);
        setImageDrawable(drawable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attachedToWindow = true;
        if (drawable != null) {
            drawable.setCallback(this);
            if (playing) {
                drawable.start();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attachedToWindow = false;
        if (drawable != null) {
            drawable.stop();
        }
    }

    public boolean isPlaying() {
        return drawable != null && drawable.isRunning();
    }

    public void setAutoRepeat(boolean repeat) {
        autoRepeat = repeat;
    }

    public void setProgress(float progress) {
        if (drawable == null) {
            return;
        }
        drawable.setProgress(progress);
    }

    public void playAnimation() {
        if (drawable == null) {
            return;
        }
        playing = true;
        if (attachedToWindow) {
            drawable.start();
        }
    }

    public void stopAnimation() {
        if (drawable == null) {
            return;
        }
        playing = false;
        if (attachedToWindow) {
            drawable.stop();
        }
    }

    public RLottieDrawable getAnimatedDrawable() {
        return drawable;
    }
}
