package cc.ibooker.zalarm.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cc.ibooker.zalarm.activity.OnePiexlActivity;
import cc.ibooker.zalarm.service.ServiceManager;

/**
 * 系统广播接收器
 *
 * @author 邹峰立
 */
public class SysBroadcastReceiver extends BroadcastReceiver {
    private boolean lock;

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SysBroadcastReceiver", "onReceive");
        if (!lock) {
            lock = true;
            if (intent != null) {
                // 开启闹钟服务
                ServiceManager.getInstance().startAlarmService(context);
                // 开启远程服务
                ServiceManager.getInstance().startRemoteService(context);

                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {// 开屏
                    // 关闭1像素Activity
                    context.sendBroadcast(new Intent("finishOnePiexlActivity"));
                    // 进入首页
                    Intent main = new Intent(Intent.ACTION_MAIN);
                    main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    main.addCategory(Intent.CATEGORY_HOME);
                    context.startActivity(main);
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {// 锁屏
                    // 开启1像素Activity
                    Intent intentOnePiexl = new Intent(context, OnePiexlActivity.class);
                    intentOnePiexl.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intentOnePiexl);
                }
            }
            lock = false;
        }
    }
}
