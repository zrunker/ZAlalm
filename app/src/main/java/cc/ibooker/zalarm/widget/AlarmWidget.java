package cc.ibooker.zalarm.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import cc.ibooker.zalarm.R;
import cc.ibooker.zalarm.service.AlarmService;

/**
 * 添加执行过程：onEnabled -> onReceive -> onUpdate -> onReceive -> onAppWidgetOptionsChanged -> onReceive
 * <p>
 * Implementation of App Widget functionality.
 */
public class AlarmWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.alarm_widget);
        views.setTextViewText(R.id.appwidget_text, widgetText);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        // 更新操作
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        Log.d("AlarmWidget", "onUpdate");
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        // Widget第一个被添加到桌面执行方法
        startAlarmService(context);

        Log.d("AlarmWidget", "onEnabled");
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        // Widget最后一个被移除执行方法

        Log.d("AlarmWidget", "onDisabled");
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        // 从屏幕移除
        Log.d("AlarmWidget", "onDeleted");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        startAlarmService(context);

        Log.d("AlarmWidget", "onReceive");
    }

    /**
     * 启动闹钟服务
     *
     * @param context 上下文对象
     */
    private void startAlarmService(Context context) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.setAction("cc.ibooker.zalarm.alarm_service");
        intent.putExtra("isOpenStartForeground", false);
        intent.putExtra("isUpdateAlarmCalendar", false);
        context.startService(intent);
    }
}

