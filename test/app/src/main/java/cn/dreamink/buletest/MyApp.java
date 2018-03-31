package cn.dreamink.buletest;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

/**
 * 作者: old样
 * 描述:
 * 上海传智播客android黑马程序员
 **/

public class MyApp extends Application {
    public static Context context;
    public static Handler mainHandler;
    public static MyApp app;
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        app = this;
        mainHandler = new Handler();
    }
}
