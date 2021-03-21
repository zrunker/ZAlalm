# ZAlalm 闹钟实例

1、杀不死的Service。

2、双进程保护Aidl。

3、Android 5.0 JobScheduler和Android 6.0 Doze模式。

4、AppWidget小组件。

5、对一些系统广播监听（开机、锁屏、安装更新APP...）。

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
