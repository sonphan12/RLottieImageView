package com.example.rlottiebenchmark;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private int mStickerSize = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            onPermissionsGranted();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 100 || grantResults.length == 0) {
            return;
        }

        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        onPermissionsGranted();
    }

    private void onPermissionsGranted() {
        mStickerSize = 240;

        StickerAdapter adapter = new StickerAdapter();
        int spanCount = 5;
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(adapter);
        adapter.setStickers(buildStickers());
    }

    private List<StickerAdapter.StickerUiModel> buildStickers() {
        final List<StickerAdapter.StickerUiModel> result = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            String stickerName = "sticker_" + (i + 1);
            int resId = getResources().getIdentifier(stickerName, "raw", getPackageName());
            copyFiletoCache(resId, stickerName + ".zls");
            result.add(new StickerAdapter.StickerUiModel(getCacheDir() + "/" + stickerName + ".zls", mStickerSize, mStickerSize));
        }
        return result;
    }

    private int getScreenWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    private void copyFiletoCache(int resourceId, String resourceName) {
        String cacheDir = getCacheDir() + "/" + resourceName;
        try {
            InputStream in = getResources().openRawResource(resourceId);
            FileOutputStream out;
            out = new FileOutputStream(cacheDir);
            byte[] buff = new byte[1024];
            int read;
            try {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            } finally {
                in.close();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}