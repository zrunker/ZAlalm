package cc.ibooker.zalarm.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import cc.ibooker.zalarm.receiver.SysBroadcastReceiver;

/**
 * Service管理类
 *
 * @author 邹峰立
 */
public class ServiceManager {
    private static ServiceManager serviceManager;
    private SysBroadcastReceiver sysBroadcastReceiver;

    public static ServiceManager getInstance() {
        if (serviceManager == null)
            synchronized (ServiceManager.class) {
                serviceManager = new ServiceManager();
            }
        return serviceManager;
    }

    /**
     * 注册广播
     *
     * @param context 上下文对象
     */
    public void registerReceiver(Context context) {
        // 开启系统广播
        if (sysBroadcastReceiver == null) {
            sysBroadcastReceiver = new SysBroadcastReceiver();
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
            Context appContext = context.getApplicationContext();
            appContext.registerReceiver(sysBroadcastReceiver, filter);
        }
    }

    /**
     * 取消广播
     *
     * @param context 上下文对象
     */
    public void unregisterReceiver(Context context) {
        if (sysBroadcastReceiver != null) {
            Context appContext = context.getApplicationContext();
            appContext.unregisterReceiver(sysBroadcastReceiver);
            sysBroadcastReceiver = null;
        }
    }

    /**
     * 开启闹钟服务
     */
    public void startAlarmService(Context context) {
//        Context appContext = context.getApplicationContext();
        Intent intentAlarm = new Intent(context, AlarmService.class);
        intentAlarm.setAction("cc.ibooker.zalarm.alarm_service");
        intentAlarm.putExtra("isOpenStartForeground", true);
        intentAlarm.putExtra("isUpdateAlarmCalendar", true);
        intentAlarm.putExtra("alarmType", AlarmService.TYPE_ONE_DAY);
        context.startService(intentAlarm);
    }

    /**
     * 开启远程服务
     */
    public void startRemoteService(Context context) {
//        Context appContext = context.getApplicationContext();
        Intent intentRemote = new Intent(context, RemoteService.class);
        intentRemote.setAction("cc.ibooker.zalarm.remote_service");
        context.startService(intentRemote);
    }
}
