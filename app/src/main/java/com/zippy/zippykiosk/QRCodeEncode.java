package com.zippy.zippykiosk;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by KB on 8/05/15.
 * Copyright 2015 Zippy.com.au.
 */
class QRCodeEncode {

    /**
     * Create bitmap representation of a QR code
     *
     * @param charsetName Character set name, eg UTF-8 or set to null
     * @return Bitmap
     */
    @Nullable
    public static Bitmap encodeAsBitmap(String content, int dimension, @Nullable String charsetName) {
        final int WHITE = 0xFFFFFFFF;
        final int BLACK = 0xFF000000;
        Bitmap bitmap = null;
        try {
            Map<EncodeHintType, Object> hints = null;
            if (charsetName != null) {
                hints = new EnumMap<>(EncodeHintType.class);
                hints.put(EncodeHintType.CHARACTER_SET, charsetName);
            }
            MultiFormatWriter writer = new MultiFormatWriter();


            BitMatrix result = writer.encode(content, BarcodeFormat.QR_CODE, dimension, dimension, hints);

            int width = result.getWidth();
            int height = result.getHeight();
            int[] pixels = new int[width * height];
            // All are 0, or black, by default
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
                }
            }

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        } catch(Exception e) {
            Log.e(e);
        }
        return bitmap;
    }
    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) { return "UTF-8"; }
        }
        return null;
    }
}
