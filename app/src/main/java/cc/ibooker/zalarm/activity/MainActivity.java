package cc.ibooker.zalarm.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.HashMap;
import java.util.Map;

import cc.ibooker.zalarm.R;
import cc.ibooker.zalarm.service.JobSchedulerService;
import cc.ibooker.zalarm.service.ServiceManager;
import cc.ibooker.zalarm.sharedpreferences.SharedpreferencesUtil;

/**
 * Android 闹钟实例：
 * <p>
 * 1、杀不死的服务Service，只能在进程存在的情况下，降低系统回收几率。
 * - A：android:priority="1000"最高权限；
 * - B:onStartCommand返回值设置，getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.ECLAIR ? START_STICKY_COMPATIBILITY : START_STICKY;
 * - C:前置服务，startForeground(int id, Notification notification);
 * - D:onDestroy()方法中重启服务。
 * <p>
 * 2、双进程保护：AIDL，开启两个Service(A和B)，运行在两个不同的进程中android:process=":remote_service"，实现A和B相互守护。
 * <p>
 * 3、Android 5.0 JobScheduler，Android 6.0 Doze模式。
 * <p>
 * 4、AppWidget小组件开发，定义倒计时小组件，在小组件中启动闹钟服务。
 * <p>
 * 5、对一些系统广播监听（开机、锁屏、安装更新APP...）
 *
 * @author 邹峰立
 */
public class MainActivity extends AppCompatActivity {
    private EditText hourEd, minuteEd;
    private final int JOB_ID = 122;
    private final int REQUEST_IGNORE_BATTERY_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ServiceManager.unregisterReceiver(this);
    }

    // 初始化控件
    private void initView() {
        hourEd = findViewById(R.id.ed_hour);
        minuteEd = findViewById(R.id.ed_minute);
        final ToggleButton tBtn = findViewById(R.id.tbtn_open_alarm);
        tBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 是否开启闹钟
                Map<String, Object> map = new HashMap<>();
                map.put("isOpenAlarmRemind", isChecked);
                SharedpreferencesUtil.getIntance().saveSharedPreferences(MainActivity.this, "StudyRemindSetting", MODE_APPEND, map);

                // 处理Android 6.0 Doze模式
                isIgnoreBatteryOption(MainActivity.this);

                // JobScheduler相关操作
                if (isChecked)
                    startJobSchedulerService();
                else
                    cancelJobScheduler();
            }
        });
        Button ensureBtn = findViewById(R.id.btn_ensure);
        ensureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 小时
                String hourStr = hourEd.getText().toString().trim();
                if (!TextUtils.isEmpty(hourStr)) {
                    int hour = Integer.parseInt(hourStr);
                    if (hour >= 0 && hour <= 24) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("hour", hour);
                        SharedpreferencesUtil.getIntance().saveSharedPreferences(MainActivity.this, "StudyRemindSetting", MODE_APPEND, map);


                        // 分钟
                        String minuteStr = minuteEd.getText().toString().trim();
                        int minute = Integer.parseInt(minuteStr);
                        if (minute >= 0 && minute <= 60) {
                            Map<String, Object> map1 = new HashMap<>();
                            map1.put("minute", minute);
                            SharedpreferencesUtil.getIntance().saveSharedPreferences(MainActivity.this, "StudyRemindSetting", MODE_APPEND, map1);


                            // 开启闹钟服务
                            ServiceManager.startAlarmService(MainActivity.this);
                            // 开启远程服务
                            ServiceManager.startRemoteService(MainActivity.this);

                            // 注册广播
                            ServiceManager.registerReceiver(MainActivity.this);

                            // 初始化控件
                            hourEd.setText("");
                            minuteEd.setText("");
                        } else {
                            Toast.makeText(MainActivity.this, "分钟只能填写0~60", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "小时只能填写0~24", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * 开启JobSchedulerService，1分钟执行一次
     */
    private void startJobSchedulerService() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, new ComponentName(getPackageName(), JobSchedulerService.class.getName()));

            builder.setPeriodic(60 * 1000); // 每隔60秒运行一次
            builder.setRequiresCharging(true);

            builder.setPersisted(true);  // 设置设备重启后，是否重新执行任务
            builder.setRequiresDeviceIdle(true);

//            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);

            if (jobScheduler != null) {
                int result = jobScheduler.schedule(builder.build());
                if (result <= 0) {
                    // If something goes wrong
                    jobScheduler.cancel(JOB_ID);
                }
            }
        }
    }

    /**
     * 取消JobScheduler
     */
    private void cancelJobScheduler() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancel(JOB_ID);
            }
        }
    }

    /**
     * 针对N以上的Doze模式
     *
     * @param activity 当前Activity
     */
    @SuppressLint("BatteryLife")
    public void isIgnoreBatteryOption(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                if (!(pm != null && pm.isIgnoringBatteryOptimizations(packageName))) {
//                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivityForResult(intent, REQUEST_IGNORE_BATTERY_CODE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IGNORE_BATTERY_CODE) {
                // 开启JobSchedulerService
                startJobSchedulerService();
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_IGNORE_BATTERY_CODE) {
                Toast.makeText(MainActivity.this, "请开启忽略电池优化~", Toast.LENGTH_LONG).show();
            }
        }
    }

}
