package cc.ibooker.zalarm.activity;

import android.app.Activity;
import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * 保活Activity管理类
 *
 * @author 邹峰立
 */
public class LiveActivityManager {
    private Context mContext;
    private WeakReference<Activity> mActivityWref;

    public static LiveActivityManager gDefualt;

    public static LiveActivityManager getInstance(Context pContext) {
        if (gDefualt == null) {
            gDefualt = new LiveActivityManager(pContext.getApplicationContext());
        }
        return gDefualt;
    }

    private LiveActivityManager(Context pContext) {
        this.mContext = pContext;
    }

    public void setActivity(Activity pActivity) {
        mActivityWref = new WeakReference<>(pActivity);
    }

    public void startActivity() {
        LiveActivity.actionToLiveActivity(mContext);
    }

    public void finishActivity() {
        //结束掉LiveActivity
        if (mActivityWref != null) {
            Activity activity = mActivityWref.get();
            if (activity != null) {
                activity.finish();
            }
        }
    }
}
