package com.xxtsoft.cppsekai;

import android.content.Intent;
import android.view.KeyEvent;
import org.libsdl.app.SDLActivity;

public class CppSekaiActivity extends SDLActivity
{
    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        // SDLActivity.onCreate 会启动 native 线程；必须在它之前释放资源
        AssetExtractor.extractIfNeeded(getApplicationContext());
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                finishAffinity();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public void startDownloadActivity() {
        Intent i = new Intent(this, DownloadActivity.class);
        startActivity(i);
    }
}
