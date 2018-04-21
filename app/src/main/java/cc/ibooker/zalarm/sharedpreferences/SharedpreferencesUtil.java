package cc.ibooker.zalarm.sharedpreferences;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * SharedPreferences管理类
 *
 * @author 邹峰立
 */
public class SharedpreferencesUtil {
    private HashMap<String, SharedPreferences> mapCache = new HashMap<>();
    private static SharedpreferencesUtil sharedpreferencesUtil;

    public static synchronized SharedpreferencesUtil getIntance() {
        if (sharedpreferencesUtil == null)
            sharedpreferencesUtil = new SharedpreferencesUtil();
        return sharedpreferencesUtil;
    }

    // 获取SharedPreferences中所有数据
    public Map<String, ?> readSharedPreferences(Context context, String name, int mode) {
        if (mode != Context.MODE_PRIVATE && mode != Context.MODE_APPEND)
            return null;
        SharedPreferences sharedPreferences = context.getSharedPreferences(name, mode);
        return sharedPreferences.getAll();
    }

    // 保存数据到SharedPreferences
    public boolean saveSharedPreferences(Context context, String name, int mode, Map<String, ?> map) {
        if (mode != Context.MODE_PRIVATE && mode != Context.MODE_APPEND)
            return false;

        SharedPreferences sharedPreferences = context.getSharedPreferences(name, mode);
        mapCache.put(name, sharedPreferences);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object obj = entry.getValue();
            if (obj instanceof String)
                editor.putString(entry.getKey(), obj.toString());
            if (obj instanceof Boolean)
                editor.putBoolean(entry.getKey(), (Boolean) obj);
            if (obj instanceof Integer)
                editor.putInt(entry.getKey(), (Integer) obj);
            if (obj instanceof Float)
                editor.putFloat(entry.getKey(), (Float) obj);
            if (obj instanceof Long)
                editor.putLong(entry.getKey(), (Long) obj);
        }

        return editor.commit();
    }

    // 清空所有SharedPreference
    public void clearAllSharedpreferences() {
        if (mapCache.size() > 0) {
            for (HashMap.Entry<String, SharedPreferences> entry : mapCache.entrySet()) {
                SharedPreferences sharedPreferences = mapCache.get(entry.getKey());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
            }
            mapCache.clear();
        }
    }

    // 清空指定SharedPreferences
    public void clearSharedPreferencesByName(String name) {
        if (mapCache.size() > 0) {
            for (HashMap.Entry<String, SharedPreferences> entry : mapCache.entrySet()) {
                if (entry.getKey().equals(name)) {
                    SharedPreferences sharedPreferences = mapCache.get(entry.getKey());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();

                    mapCache.remove(entry.getKey());
                    break;
                }
            }
        }
    }

}
