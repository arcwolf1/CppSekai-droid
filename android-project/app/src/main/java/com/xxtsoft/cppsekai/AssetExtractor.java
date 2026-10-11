package com.xxtsoft.cppsekai;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.*;

/**
 * 首次启动时把 APK 内 assets/ 释放到 app 外部私有目录。
 * 路径 = Context.getExternalFilesDir(null)，即
 *   /storage/emulated/0/Android/data/com.xxtsoft.cppsekai/files/
 * 与 C++ 层的 SDL_AndroidGetExternalStoragePath() 一致，
 * 从 Android 4.4 到 15 都可写且无需权限。
 *
 * 用标记文件避免重复释放——只在首次安装或版本更新后触发。
 */
public class AssetExtractor {
    private static final String TAG = "CppSekai";
    private static final String MARKER_NAME = ".extracted_v1";

    public static void extractIfNeeded(Context ctx) {
        File root = getTargetRoot(ctx);
        if (!root.exists() && !root.mkdirs()) {
            Log.e(TAG, "Cannot create " + root);
            return;
        }

        File marker = new File(root, MARKER_NAME);
        if (marker.exists()) {
            Log.i(TAG, "Assets already extracted, skip.");
            return;
        }

        Log.i(TAG, "Extracting assets to " + root + " ...");
        long start = System.currentTimeMillis();
        int count = extractDir(ctx.getAssets(), "", root);
        long elapsed = System.currentTimeMillis() - start;
        Log.i(TAG, "Extracted " + count + " files in " + elapsed + "ms");

        try {
            new FileOutputStream(marker).close();
        } catch (IOException e) {
            Log.w(TAG, "Cannot write marker: " + e);
        }
    }

    /** 目标目录：app 的外部私有目录。 */
    private static File getTargetRoot(Context ctx) {
        File f = ctx.getExternalFilesDir(null);
        if (f == null) {
            // 极端回退：外部存储不可用
            f = new File("/sdcard/Android/data/com.xxtsoft.cppsekai/files");
        }
        return f;
    }

    /** 递归遍历 assets 目录，返回释放的文件数。 */
    private static int extractDir(AssetManager am, String assetPath, File outDir) {
        int count = 0;
        String[] children;
        try {
            children = am.list(assetPath);
        } catch (IOException e) {
            Log.e(TAG, "list failed for " + assetPath + ": " + e);
            return 0;
        }
        if (children == null || children.length == 0) return 0;

        for (String child : children) {
            String childAssetPath = assetPath.isEmpty() ? child : assetPath + "/" + child;
            File outFile = new File(outDir, child);

            // 先试当目录处理
            String[] grandChildren;
            try {
                grandChildren = am.list(childAssetPath);
            } catch (IOException e) {
                grandChildren = null;
            }

            if (grandChildren != null && grandChildren.length > 0) {
                if (!outFile.exists()) outFile.mkdirs();
                count += extractDir(am, childAssetPath, outFile);
            } else {
                // 当文件处理
                if (copyFile(am, childAssetPath, outFile)) count++;
            }
        }
        return count;
    }

    private static boolean copyFile(AssetManager am, String assetPath, File outFile) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = am.open(assetPath);
            out = new FileOutputStream(outFile);
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "copy failed " + assetPath + " -> " + outFile + ": " + e);
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
        }
    }
}
