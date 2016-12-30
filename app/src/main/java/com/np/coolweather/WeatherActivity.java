package com.np.coolweather;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.np.coolweather.gson.Forecast;
import com.np.coolweather.gson.Weather;
import com.np.coolweather.utils.AppConstant;
import com.np.coolweather.utils.HttpUtil;
import com.np.coolweather.utils.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private TextView tvTitleCity;
    private TextView tvTitleUpdateTime;

    private TextView tvNowDegreeText;
    private TextView tvNowDegreeInfoText;

    private LinearLayout lLayoutForecast;

    private TextView tvAqiText;
    private TextView tvAqiPm25Text;

    private TextView tvSuggestionComfortText;
    private TextView tvSuggestionCarWashText;
    private TextView tvSuggestionSportText;

    private SharedPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        initViews();
        mPref = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = mPref.getString("Weather", null);
        if (weatherString != null) {
            // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            // 无缓存时去服务器查询天气
            String weatherId = getIntent().getStringExtra("weather_id");
            lLayoutForecast.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
    }

    /** 根据天气 id 请求城市天气信息 */
    private void requestWeather(String weatherId) {
        // 请求参数
        String requestParam = "?cityid=" + weatherId + "&key=" + AppConstant.BASE_WEATHER_KEY;
        String weatherUrl = AppConstant.BASE_URL + AppConstant.WEATHER + requestParam;
        HttpUtil.sendOKHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseString = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseString);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor edit = mPref.edit();
                            edit.putString("Weather", responseString);
                            edit.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /** 处理并展示 Weather 实体类中的数据 */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        tvTitleCity.setText(cityName);
        tvTitleUpdateTime.setText(updateTime);
        tvNowDegreeText.setText(degree);
        tvNowDegreeInfoText.setText(weatherInfo);

        lLayoutForecast.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, lLayoutForecast, false);
            TextView tvDateText = (TextView) view.findViewById(R.id.forecast_item_date_text);
            TextView tvInfoText = (TextView) view.findViewById(R.id.forecast_item_info_text);
            TextView tvMaxText = (TextView) view.findViewById(R.id.forecast_item_max_text);
            TextView tvMinText = (TextView) view.findViewById(R.id.forecast_item_min_text);
            tvDateText.setText(forecast.date);
            tvInfoText.setText(forecast.more.info);
            tvMaxText.setText(forecast.temperature.max);
            tvMinText.setText(forecast.temperature.min);
            lLayoutForecast.addView(view);
        }

        if (weather.aqi != null) {
            tvAqiText.setText(weather.aqi.city.aqi);
            tvAqiPm25Text.setText(weather.aqi.city.pm25);
        } else {
            tvAqiText.setText("无");
            tvAqiPm25Text.setText("无");
        }

        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "汽车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        tvSuggestionComfortText.setText(comfort);
        tvSuggestionCarWashText.setText(carWash);
        tvSuggestionSportText.setText(sport);
        lLayoutForecast.setVisibility(View.VISIBLE);
    }

    private void initViews() {
        tvTitleCity = (TextView) findViewById(R.id.title_city);
        tvTitleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        tvNowDegreeText = (TextView) findViewById(R.id.now_degree_text);
        tvNowDegreeInfoText = (TextView) findViewById(R.id.now_degree_info_text);
        lLayoutForecast = (LinearLayout) findViewById(R.id.forecast_layout);
        tvAqiText = (TextView) findViewById(R.id.aqi_text);
        tvAqiPm25Text = (TextView) findViewById(R.id.aqi_pm25_text);
        tvSuggestionComfortText = (TextView) findViewById(R.id.suggestion_comfort_text);
        tvSuggestionCarWashText = (TextView) findViewById(R.id.suggestion_car_wash_text);
        tvSuggestionSportText = (TextView) findViewById(R.id.suggestion_sport_text);
    }
}
