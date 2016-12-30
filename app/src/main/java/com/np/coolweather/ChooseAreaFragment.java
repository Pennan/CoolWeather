package com.np.coolweather;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.np.coolweather.db.City;
import com.np.coolweather.db.County;
import com.np.coolweather.db.Province;
import com.np.coolweather.utils.AppConstant;
import com.np.coolweather.utils.HttpUtil;
import com.np.coolweather.utils.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static org.litepal.crud.DataSupport.findAll;

public class ChooseAreaFragment extends Fragment {

    private static final int LEVEL_PROVINCE = 0;
    private static final int LEVEL_CITY = 1;
    private static final int LEVEL_COUNTY = 2;

    /** 获取所有省份的 Url */
    private static final String CHINA_URL = AppConstant.BASE_URL + AppConstant.CHINA;

    private ProgressDialog mDialog;

    private TextView tvTitle;
    private Button btnBack;
    private ListView mListView;

    private ArrayAdapter<String> mArrayAdapter;
    private List<String> mDataList = new ArrayList<>();

    private List<Province> provinces;
    private List<City> cities;
    private List<County> counties;

    /** 选中的省份 */
    private Province selectedProvince;
    /** 选中的城市 */
    private City selectedCity;

    /** 当前选中的级别 */
    private int currentLevel;

    public ChooseAreaFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose_area, container, false);
        tvTitle = (TextView) view.findViewById(R.id.tv_choose_area_title);
        btnBack = (Button) view.findViewById(R.id.btn_choose_area_back);
        mListView = (ListView) view.findViewById(R.id.lv_choose_area);

        mArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, mDataList);
        mListView.setAdapter(mArrayAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (currentLevel) {
                    case LEVEL_PROVINCE: // 点击查询该省所有市
                        selectedProvince = provinces.get(position);
                        queryCities();
                        break;
                    case LEVEL_CITY: // 点击查询该市所有县
                        selectedCity = cities.get(position);
                        queryCounties();
                        break;
                    case LEVEL_COUNTY: // 点击查询该县天气信息
                        String weatherId = counties.get(position).getWeatherId();
                        if (getActivity() instanceof MainActivity) {
                            Intent intent = new Intent(getActivity(), WeatherActivity.class);
                            intent.putExtra("weather_id", weatherId);
                            startActivity(intent);
                            getActivity().finish();
                        } else if (getActivity() instanceof WeatherActivity) {
                            WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                            // 关闭侧滑菜单
                            weatherActivity.drawerLayout.closeDrawers();
                            // 开启刷新控件
                            weatherActivity.swipeRefreshLayout.setRefreshing(true);
                            // 执行天气信息请求
                            weatherActivity.requestWeather(weatherId);
                        }
                        break;
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentLevel) {
                    case LEVEL_CITY:
                        queryProvinces();
                        break;
                    case LEVEL_COUNTY:
                        queryCities();
                        break;
                }
            }
        });

        queryProvinces();
    }

    /** 查询所有的县，优先从数据库查询，如果没有查询到再去服务器上查询 */
    private void queryCounties() {
        tvTitle.setText(selectedCity.getCityName());
        btnBack.setVisibility(View.VISIBLE);
        counties = DataSupport.where("cityid = ?",
                String.valueOf(selectedCity.getId())).find(County.class);
        if (counties.size() > 0) {
            mDataList.clear();
            for (County county : counties) {
                mDataList.add(county.getCountyName());
            }
            mArrayAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = CHINA_URL + "/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /** 查询所有的市，优先从数据库查询，如果没有查询到再去服务器上查询 */
    private void queryCities() {
        tvTitle.setText(selectedProvince.getProvinceName());
        btnBack.setVisibility(View.VISIBLE);
        cities = DataSupport.where("provinceid = ?",
                String.valueOf(selectedProvince.getId())).find(City.class);
        if (cities.size() > 0) {
            mDataList.clear();
            for (City city : cities) {
                mDataList.add(city.getCityName());
            }
            mArrayAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = CHINA_URL + "/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /** 查询所有的省，优先从数据库查询，如果没有查询到再去服务器上查询 */
    private void queryProvinces() {
        tvTitle.setText("中国");
        btnBack.setVisibility(View.INVISIBLE);
        provinces = findAll(Province.class);
        if (provinces.size() > 0) {
            mDataList.clear();
            for (Province province : provinces) {
                mDataList.add(province.getProvinceName());
            }
            mArrayAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = CHINA_URL;
            queryFromServer(address, "province");
        }
    }

    /** 根据传入的地址和类型从服务器上查询省市县数据 */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOKHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                switch (type) {
                    case "province":
                        result = Utility.handleProvinceResponse(responseText);
                        break;
                    case "city":
                        result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                        break;
                    case "county":
                        result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                        break;
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            switch (type) {
                                case "province":
                                    queryProvinces();
                                    break;
                                case "city":
                                    queryCities();
                                    break;
                                case "county":
                                    queryCounties();
                                    break;
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(), "加载失败.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgressDialog() {
        if (mDialog == null) {
            mDialog = new ProgressDialog(getActivity());
            mDialog.setMessage("玩命加载中...");
            mDialog.setCanceledOnTouchOutside(false);
        }
        mDialog.show();
    }

    private void closeProgressDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }
}
