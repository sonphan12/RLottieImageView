package com.example.rlottiebenchmark;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StickerAdapter adapter = new StickerAdapter();
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
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
            result.add(new StickerAdapter.StickerUiModel(resId, 600, 600));
        }
        return result;
    }
}