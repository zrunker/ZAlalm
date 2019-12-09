package cc.ibooker.zalarm.service;

import android.content.Context;
import android.content.Intent;

/**
 * Service管理类
 *
 * @author 邹峰立
 */
public class ServiceManager {

    /**
     * 开启闹钟服务
     */
    public static void startAlarmService(Context context) {
        Context appContext = context.getApplicationContext();
        Intent intentAlarm = new Intent(appContext, AlarmService.class);
        intentAlarm.setAction("cc.ibooker.zalarm.alarm_service");
        intentAlarm.putExtra("isOpenStartForeground", true);
        intentAlarm.putExtra("isUpdateAlarmCalendar", true);
        intentAlarm.putExtra("alarmType", AlarmService.TYPE_ONE_DAY);
        appContext.startService(intentAlarm);
    }

    /**
     * 开启远程服务
     */
    public static void startRemoteService(Context context) {
        Context appContext = context.getApplicationContext();
        Intent intentRemote = new Intent(appContext, RemoteService.class);
        intentRemote.setAction("cc.ibooker.zalarm.remote_service");
        appContext.startService(intentRemote);
    }
}
