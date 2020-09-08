package com.example.rlottiebenchmark;

import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int STICKER_SIZE = 240;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StickerAdapter adapter = new StickerAdapter();
        int spanCount = getScreenWidth() / AndroidUtilities.dp(STICKER_SIZE);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(adapter);
        adapter.setStickers(buildStickers());
    }

    private List<StickerAdapter.StickerUiModel> buildStickers() {
        final List<StickerAdapter.StickerUiModel> result = new ArrayList<>();
        for (int i = 0; i < 429; i++) {
            String stickerName = "sticker_" + (i + 1);
            int resId = getResources().getIdentifier(stickerName, "raw", getPackageName());
            result.add(new StickerAdapter.StickerUiModel(resId, STICKER_SIZE, STICKER_SIZE));
        }
        return result;
    }

    private int getScreenWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }
}