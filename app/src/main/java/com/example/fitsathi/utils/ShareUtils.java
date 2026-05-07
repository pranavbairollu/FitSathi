package com.example.fitsathi.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.view.View;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareUtils {

    public static Bitmap getBitmapFromView(View view) {
        // Define a capacity for the bitmap (e.g. 1080x1920 for Stories)
        // If the view hasn't been measured, we might need to measure it
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }
        
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public static Uri saveBitmapToCache(Context context, Bitmap bitmap) {
        File cachePath = new File(context.getCacheDir(), "images");
        cachePath.mkdirs();
        try {
            File file = new File(cachePath, "weekly_progress_" + System.currentTimeMillis() + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void shareImage(Context context, Uri uri, String platform) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if ("whatsapp".equalsIgnoreCase(platform)) {
            intent.setPackage("com.whatsapp");
        } else if ("instagram".equalsIgnoreCase(platform)) {
            intent.setPackage("com.instagram.android");
        }

        context.startActivity(Intent.createChooser(intent, "Share Progress via"));
    }
}
