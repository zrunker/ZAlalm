package cc.ibooker.zalarm.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import cc.ibooker.zalarm.IAlarmAidlInterface;
import cc.ibooker.zalarm.R;
import cc.ibooker.zalarm.receiver.AlarmReceiver;
import cc.ibooker.zalarm.sharedpreferences.SharedpreferencesUtil;
import cc.ibooker.zalarm.widget.AlarmWidget;

/**
 * 闹钟服务
 * <p>
 * StartService：OnCreate -> onStartCommand
 *
 * @author 邹峰立
 */
public class AlarmService extends Service {
    private boolean isCreate = false;
    private int startArgFlags;
    private boolean isOpenStartForeground = true;
    private boolean isOpenAlarmRemind;

    // 与闹钟相关
    private AlarmManager alarmManager;
    private Calendar calendar;
    private PendingIntent sender;

    public static final int TYPE_TEMP = 1, TYPE_ONE_DAY = 2;

    private MyAlarmConn conn;
    private MyAlarmBinder binder;

    private Timer timer;
    private SimpleDateFormat simpleDateFormat;
    private RemoteViews remoteViews;
    private AppWidgetManager appWidgetManager;
    private ComponentName provider;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // Service服务创建，只会执行一次
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

        // 与闹钟相关
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Intent intent = new Intent(this, AlarmReceiver.class);
        sender = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        conn = new MyAlarmConn();
        binder = new MyAlarmBinder();
    }

    // Service服务启动，可能执行多次
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isCreate) {
            // 判断是否需要开启提醒
            Map<String, ?> map = SharedpreferencesUtil.getIntance().readSharedPreferences(this, "StudyRemindSetting", MODE_APPEND);
            isOpenAlarmRemind = Boolean.parseBoolean(map.get("isOpenAlarmRemind").toString());
            if (isOpenAlarmRemind) {// 开启服务提醒
                // 绑定远程服务
                bindRemoteService();

                boolean isUpdateAlarmCalendar = false;// 标记是否需要更新闹钟时间
                if (intent != null) {
                    isOpenStartForeground = intent.getBooleanExtra("isOpenStartForeground", true);
                    isUpdateAlarmCalendar = intent.getBooleanExtra("isUpdateAlarmCalendar", false);
                }
                // 开启前置服务
                if (isOpenStartForeground) {
                    startForeground(1111, new Notification());
                } else {
                    stopForeground(true);
                }

                // 更新闹钟时间
                if (isUpdateAlarmCalendar) {
                    int alarmType = intent.getIntExtra("alarmType", -1);
                    // 获取时分
                    int hour = -1;
                    int minute = -1;
                    switch (alarmType) {
                        case TYPE_TEMP:// 临时闹钟 - 从Intent中获取
                            hour = intent.getIntExtra("hour", -1);
                            minute = intent.getIntExtra("minute", -1);
                            break;
                        case TYPE_ONE_DAY:// 每天 - 从SharedPreference中获取
                            hour = Integer.parseInt(map.get("hour").toString());
                            minute = Integer.parseInt(map.get("minute").toString());
                            break;
                    }

                    updateAlarm(hour, minute, alarmType);
                }
            } else {// 关闭闹钟服务
                closeAlarm();
            }
        }
        return startArgFlags;
    }

    /**
     * 更新闹钟时间
     *
     * @param hour   时
     * @param minute 分
     * @param type   类型
     */
    private void updateAlarm(int hour, int minute, int type) {
        if (hour >= 0 && minute >= 0 && alarmManager != null) {
            // 取消闹钟
            alarmManager.cancel(sender);

            // 处理时间
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // 防止设置的时间戳比当前系统时间戳小而响应闹钟问题
            if (System.currentTimeMillis() > calendar.getTimeInMillis()) {
                calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);
            }

            // 重新设置闹钟
            switch (type) {
                case TYPE_TEMP:// 临时
                    // 开启闹钟
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
                    }
                    break;

                case TYPE_ONE_DAY:// 每天
                    // 开启闹钟
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 100, sender);
                    } else {
                        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 24 * 60 * 60 * 1000, sender);
                    }
                    break;
            }

            // 开启定时器 - 一分钟更新一次Widget
            if (timer == null)
                timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String widgetText = getString(R.string.appwidget_text);
                    long time = calendar.getTimeInMillis() - System.currentTimeMillis() - TimeZone.getDefault().getRawOffset();
                    if (time != 0) {
                        if (simpleDateFormat == null)
                            simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
                        String formatTime = simpleDateFormat.format(time);
                        String[] strs = formatTime.split(":");
                        widgetText = strs[0] + "时\n" + strs[1] + "分";
                    }
                    updateWidget(widgetText);
                }
            }, 0, 1000 * 60);
        }
    }

    /**
     * 关闭闹钟服务
     */
    private void closeAlarm() {
        // 关闭闹钟
        if (alarmManager != null) {
            alarmManager.cancel(sender);
            alarmManager = null;
        }

        // 销毁定时器
        timer = null;

        // 关闭前置服务
        isOpenStartForeground = false;
        stopForeground(true);

        // 关闭远程服务
        stopRemoteService();
        unBindRemoteService();

        // 关闭自身
        stopSelf();
    }

    // Service销毁
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 销毁定时器
        timer = null;

        if (isOpenAlarmRemind) {
            // 启动自身
            startService(new Intent(this, AlarmService.class));

            // 启动远程服务
            startRemoteService();
            // 绑定远程服务
            bindRemoteService();
        }
    }

    /**
     * 更新Widget界面
     *
     * @param widgetText Widget显示内容
     */
    private void updateWidget(String widgetText) {
        if (remoteViews == null)
            remoteViews = new RemoteViews(getPackageName(), R.layout.alarm_widget);
        remoteViews.setTextViewText(R.id.appwidget_text, widgetText);
        if (appWidgetManager == null)
            appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        if (provider == null)
            provider = new ComponentName(getApplicationContext(), AlarmWidget.class);
        appWidgetManager.updateAppWidget(provider, remoteViews);
    }

    /**
     * 自定义Binder
     */
    class MyAlarmBinder extends IAlarmAidlInterface.Stub {
        @Override
        public String getServiceName() {
            return AlarmService.class.getSimpleName();
        }
    }

    /**
     * 自定义ServiceConnection
     */
    class MyAlarmConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(AlarmService.this, "闹钟服务", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (isOpenAlarmRemind) {
                // 启动远程服务
                startRemoteService();
                // 绑定远程服务
                bindRemoteService();
            }
        }

    }

    /**
     * 启动远程服务
     */
    private void startRemoteService() {
        Intent intent = new Intent(this, RemoteService.class);
        intent.setAction("cc.ibooker.zalarm.remote_service");
        startService(intent);
    }

    /**
     * 绑定远程服务
     */
    private void bindRemoteService() {
        Intent intent = new Intent(this, RemoteService.class);
        intent.setAction("cc.ibooker.zalarm.remote_service");
        bindService(intent, conn, Context.BIND_IMPORTANT);
    }

    /**
     * 关闭远程服务
     */
    private void stopRemoteService() {
        Intent intent = new Intent(this, RemoteService.class);
        intent.setAction("cc.ibooker.zalarm.remote_service");
        stopService(intent);
    }

    /**
     * 解绑远程服务
     */
    private void unBindRemoteService() {
        if (isOpenAlarmRemind)
            unbindService(conn);
    }
}
