package cc.ibooker.zalarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import cc.ibooker.zalarm.activity.AlarmActivity;

/**
 * 闹钟广播
 *
 * @author 邹峰立
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "闹钟响了");
        Toast.makeText(context, "闹钟响了", Toast.LENGTH_LONG).show();

        // 进入闹钟操作界面
        Intent alaramIntent = new Intent(context, AlarmActivity.class);
        alaramIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(alaramIntent);
    }
}
