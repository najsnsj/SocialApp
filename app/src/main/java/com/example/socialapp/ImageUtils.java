package com.example.socialapp;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public class ImageUtils {

    public static Bitmap fileToBitmap(String filePath) {  // 이미지(파일경로) -> 비트맵
        File imgFile = new File(filePath);
        if (imgFile.exists()) {
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }
        return null;
    }

    public static Bitmap uriToBitmap(Context context, Uri uri) {    // 이미지(URL) -> 비트맵
        try {
            ContentResolver contentResolver = context.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String uriToBase64(Context context, Uri uri) {    // 이미지(URL) -> base64
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap base64ToBitmap(String base64Str) {     // base64 -> 비트맵
        byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }


    public static String bitmapToBase64(Bitmap bitmap) {    // 비트맵 -> base64
        if (bitmap == null) { return null; }
        // Bitmap을 바이트 배열로 변환
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream); // PNG 또는 JPEG 형식으로 압축
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // 바이트 배열을 Base64 문자열로 인코딩
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

}

