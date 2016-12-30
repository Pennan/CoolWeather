package com.np.coolweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.np.coolweather.gson.Weather;
import com.np.coolweather.utils.AppConstant;
import com.np.coolweather.utils.HttpUtil;
import com.np.coolweather.utils.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 后台自动更新天气
 */
public class AutoUpdateService extends Service {

    private SharedPreferences mPrefs;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateWeather();
        updateBingPic();
        startTastManager();
        return super.onStartCommand(intent, flags, startId);
    }

    /** 开启一个定时任务,每隔 8 个小时刷新一次天气信息 */
    private void startTastManager() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000; // 这是 8 小时的毫秒数
        long triggerAtMillis = SystemClock.elapsedRealtime() + anHour;
        Intent intent = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);
        alarmManager.cancel(pi); // 取消上个匹配的定时任务
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pi);
    }

    /** 更新必应每日一图 */
    private void updateBingPic() {
        String address = AppConstant.BASE_URL + AppConstant.BING_PIC;
        HttpUtil.sendOKHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor edit = mPrefs.edit();
                edit.putString("ping_pic", bingPic);
                edit.apply();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    /** 更新天气信息 */
    private void updateWeather() {
        final String weatherString = mPrefs.getString("Weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String requestParameters = "?cityid=" + weather.basic.weatherId +
                    "&key=" + AppConstant.BASE_WEATHER_KEY;
            String address = AppConstant.BASE_URL + AppConstant.WEATHER + requestParameters;
            HttpUtil.sendOKHttpRequest(address, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseString = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseString);
                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor edit = mPrefs.edit();
                        edit.putString("Weather", responseString);
                        edit.apply();
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
