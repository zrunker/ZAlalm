package cc.ibooker.zalarm.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import java.util.Calendar;

import cc.ibooker.zalarm.service.AlarmService;

/**
 * 闹钟响应界面 - 注意两个权限
 * <p>
 * <!--震动权限-->
 * <uses-permission android:name="android.permission.VIBRATE" />
 * <!--解锁权限-->
 * <uses-permission android:name="android.permission.WAKE_LOCK" />
 *
 * @author 邹峰立
 */
public class AlarmActivity extends AppCompatActivity {
    private PowerManager.WakeLock mWakelock;
    private MediaPlayer mMediaPlayer;
    private Vibrator mVibrator;
    private AlertDialog.Builder dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= (WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        startMedia();
        startVibrator();
        createDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 唤醒屏幕
        acquireWakeLock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseWakeLock();
    }

    /**
     * 唤醒屏幕
     */
    private void acquireWakeLock() {
        if (mWakelock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                mWakelock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                                | PowerManager.SCREEN_DIM_WAKE_LOCK,
                        this.getClass().getCanonicalName());
                mWakelock.acquire(5 * 60 * 1000L /*5 minutes*/);
            }
        }
    }

    /**
     * 释放锁屏
     */
    private void releaseWakeLock() {
        if (mWakelock != null && mWakelock.isHeld()) {
            mWakelock.release();
            mWakelock = null;
        }
    }

    /**
     * 开始播放铃声
     */
    private void startMedia() {
        try {
            if (mMediaPlayer == null)
                mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)); // 铃声类型为默认闹钟铃声
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 震动 - 想设置震动大小可以通过改变pattern来设定，如果开启时间太短，震动效果可能感觉不到
     */
    private void startVibrator() {
        if (mVibrator == null)
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null) {
            long[] pattern = {500, 1000, 500, 1000}; // 停止 开启 停止 开启
            mVibrator.vibrate(pattern, 0);
        }
    }

    // 创建Dialog
    private void createDialog() {
        if (dialog == null)
            dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.ic_launcher)
                    .setTitle("每日学习提醒")
                    .setMessage("您设定的闹钟提醒时间到了!!!")
                    .setPositiveButton("推迟10分钟", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            tenMRemind();
                            mMediaPlayer.stop();
                            mVibrator.cancel();
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mMediaPlayer.stop();
                            mVibrator.cancel();
                            dialog.dismiss();
                            finish();
                        }
                    });
        dialog.show();
    }

    /**
     * 推迟10分钟提醒
     */
    private void tenMRemind() {
        // 设置时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 10);

        // 启动临时闹钟服务
        Intent intentAlarm = new Intent(this, AlarmService.class);
        intentAlarm.setAction("cc.ibooker.zalarm.alarm_service");
        intentAlarm.putExtra("isOpenStartForeground", true);
        intentAlarm.putExtra("isUpdateAlarmCalendar", true);
        intentAlarm.putExtra("alarmType", AlarmService.TYPE_TEMP);
        intentAlarm.putExtra("hour", calendar.get(Calendar.HOUR));
        intentAlarm.putExtra("minute", calendar.get(Calendar.MINUTE));
        startService(intentAlarm);
    }
}
