package com.np.coolweather.db;

import org.litepal.crud.DataSupport;

/**
 * 县
 */
public class County extends DataSupport {
    private int di;
    private String countyName;
    private String weatherId;
    private int cityId;

    public int getDi() {
        return di;
    }

    public void setDi(int di) {
        this.di = di;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public String getWeatherId() {
        return weatherId;
    }

    public void setWeatherId(String weatherId) {
        this.weatherId = weatherId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }
}
