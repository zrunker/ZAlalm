package cc.ibooker.zalarm.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * JobSchedulerService定时任务，启动闹钟服务
 *
 * @author 邹峰立
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobSchedulerService extends JobService {
    private static final int MESSAGE_ID_TASK = 0x01;

    private MyHandler myHandler = new MyHandler(this);

    static class MyHandler extends Handler {

        private WeakReference<JobSchedulerService> mWeakRef;

        MyHandler(JobSchedulerService jobSchedulerService) {
            this.mWeakRef = new WeakReference<>(jobSchedulerService);
        }

        @Override
        public void handleMessage(Message msg) {
            // 开启闹钟服务
            Intent intent = new Intent(mWeakRef.get(), AlarmService.class);
            intent.setAction("cc.ibooker.zalarm.alarm_service");
            intent.putExtra("isOpenStartForeground", false);
            intent.putExtra("isUpdateAlarmCalendar", false);
            mWeakRef.get().startService(intent);

            // 通知系统当前任务已完成
            mWeakRef.get().jobFinished((JobParameters) msg.obj, false);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Message message = Message.obtain();
        message.obj = params;
        message.what = MESSAGE_ID_TASK;
        myHandler.sendMessage(message);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        myHandler.removeMessages(MESSAGE_ID_TASK);
        return false;
    }
}
