package cc.ibooker.zalarm.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Map;

import cc.ibooker.zalarm.IAlarmAidlInterface;
import cc.ibooker.zalarm.receiver.SysBroadcastReceiver;
import cc.ibooker.zalarm.sharedpreferences.SharedpreferencesUtil;

/**
 * 远程服务，用于与闹钟服务AlarmService，进行双进程保护。
 * <p>
 * 1、开启两个不同进程的服务，android:process。
 * 2、在服务启动之后，绑定两一个服务。
 *
 * @author 邹峰立
 */
public class RemoteService extends Service {
    private int startArgFlags;
    private boolean isOpenAlarmRemind;
    private boolean isCreate;

    private MyRemoteConn conn;
    private MyRemoteBinder binder;

    // 注册广播
    private void registerReceiver() {
        // 开启系统广播
        SysBroadcastReceiver sysBroadcastReceiver = new SysBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        // 注册开机广播
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        // 注册网络状态更新
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        // 注册电池电量变化
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        // 注册应用安装状态变化
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        // 注册屏幕亮度变化广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 注册锁屏广播
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(sysBroadcastReceiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        isCreate = true;
        init();
    }

    // 初始化
    private void init() {
        // 保证内存不足，杀死会重新创建
        startArgFlags = getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.ECLAIR ? START_STICKY_COMPATIBILITY : START_STICKY;

        conn = new MyRemoteConn();
        binder = new MyRemoteBinder();

        // 注册系统广播
        registerReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isCreate) {
            // 判断是否需要开启提醒
            Map<String, ?> map = SharedpreferencesUtil.getIntance().readSharedPreferences(this, "StudyRemindSetting", MODE_APPEND);
            isOpenAlarmRemind = Boolean.parseBoolean(map.get("isOpenAlarmRemind").toString());

            if (isOpenAlarmRemind) {
                // 绑定闹钟服务
                bindAlarmService();
            }
        }
        return super.onStartCommand(intent, startArgFlags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isOpenAlarmRemind) {
            // 启动闹钟服务
            startAlarmService();
            // 绑定闹钟服务
            bindAlarmService();
        }
    }

    /**
     * 自定义Binder
     */
    class MyRemoteBinder extends IAlarmAidlInterface.Stub {
        @Override
        public String getServiceName() {
            return RemoteService.class.getSimpleName();
        }
    }

    /**
     * 自定义ServiceConnection
     */
    class MyRemoteConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
//            Toast.makeText(RemoteService.this, "远程服务", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (isOpenAlarmRemind) {
                // 启动闹钟服务
                startAlarmService();
                // 绑定闹钟服务
                bindAlarmService();
            }
        }

    }

    /**
     * 开启闹钟服务
     */
    private void startAlarmService() {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction("cc.ibooker.zalarm.alarm_service");
        intent.putExtra("isOpenStartForeground", false);
        intent.putExtra("isUpdateAlarmCalendar", false);
        startService(intent);
    }

    /**
     * 绑定闹钟服务
     */
    private void bindAlarmService() {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction("cc.ibooker.zalarm.alarm_service");
        bindService(intent, conn, Context.BIND_IMPORTANT);
    }
}
