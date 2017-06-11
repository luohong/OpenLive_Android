package qsbk.app.play.utils;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.blankj.utilcode.util.EncryptUtils;

import qsbk.app.play.AppController;

/**
 * Created by luohong on 2017/6/11.
 */

public class PhoneUtils {

    private static String CACHE_ANDROID_ID = null;

    public static String getAndroidId() {
        if(CACHE_ANDROID_ID != null){
            return CACHE_ANDROID_ID;
        }
        String deviceID = null;
        // 获取设备码
        TelephonyManager tManager = (TelephonyManager) AppController.instance()
                .getSystemService(Context.TELEPHONY_SERVICE);

        //deviceId 有些山寨机器可能会是一样的，正规厂商的id应该不一样
        //Android_ID每次系统重新刷机一次，会随机产生一个
        deviceID = "\"DEVICEID\":\"" + tManager.getDeviceId() + "\"-" + "\"ANDROID_ID\":\""
                + Settings.Secure.getString(AppController.instance().getContentResolver(), Settings.Secure.ANDROID_ID) + "\"";

        CACHE_ANDROID_ID = "IMEI_" + EncryptUtils.encryptMD5(deviceID.getBytes());

        return CACHE_ANDROID_ID;
    }

}
