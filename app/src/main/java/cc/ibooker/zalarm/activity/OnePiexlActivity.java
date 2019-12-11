package cc.ibooker.zalarm.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

/**
 * 一像素Activity
 *
 * @author 邹峰立
 */
public class OnePiexlActivity extends AppCompatActivity {
    public static final String TAG = OnePiexlActivity.class.getSimpleName();
    private BroadcastReceiver finishReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        Window window = getWindow();
        // 放在左上角
        window.setGravity(Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams attributes = window.getAttributes();
        // 宽高设计为1个像素
        attributes.width = 1;
        attributes.height = 1;
        // 起始坐标
        attributes.x = 0;
        attributes.y = 0;
        window.setAttributes(attributes);

        // 注册广播
        finishReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        registerReceiver(finishReceiver, new IntentFilter("finishOnePiexlActivity"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkScreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unregisterReceiver(finishReceiver);
    }

    /**
     * 检查屏幕状态  isScreenOn为true  屏幕“亮”结束该Activity
     */
    private void checkScreen() {
        PowerManager pm = (PowerManager) OnePiexlActivity.this.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();
        if (isScreenOn) {
            finish();
        }
    }

}
