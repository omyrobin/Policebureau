package com.administration.policebureau.map;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.administration.policebureau.BaseActivity;
import com.administration.policebureau.R;
import com.administration.policebureau.adapter.InfoWinAdapter;
import com.administration.policebureau.util.AMapUtil;
import com.administration.policebureau.util.ToastUtil;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.TranslateAnimation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.Tip;

import java.util.List;

import butterknife.BindView;

/**
 * Created by omyrobin on 2017/11/3.
 */

public class MapActivity extends BaseActivity implements AMap.OnCameraChangeListener, AMap.OnMyLocationChangeListener,GeocodeSearch.OnGeocodeSearchListener,Inputtips.InputtipsListener  {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.toolbar_title)
    TextView titleTv;
    @BindView(R.id.toolbar_action)
    TextView actionTv;
    @BindView(R.id.map)
    MapView mMapView;
    //初始化地图控制器对象
    public AMap aMap;
    public MyLocationStyle myLocationStyle;
    //定义一个UiSettings对象
    private UiSettings mUiSettings;
    private LatLonPoint lp;
    private GeocodeSearch geocoderSearch;
    private Marker screenMarker;
    private double mLocationLatitude, mLocationLongitude;
    private boolean isFirst = true;
    private String addressName;//逆地理编码得到的地址
    private boolean isFirstAdd = true;
    private Bundle savedInstanceState;
    private double latitude, longitude;

    public static void newInstance(Context context, String lp,String addressName){
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra("location", lp);
        intent.putExtra("addressName", addressName);
        context.startActivity(intent);
    }

    @Override
    protected void getExtra() {
        super.getExtra();
        addressName = getIntent().getExtras().getString("addressName");
        String lp = getIntent().getExtras().getString("location");
        String [] location = lp.split(",");
        latitude = Double.parseDouble(location[0]);
        longitude = Double.parseDouble(location[1]);
    }

    @Override
    protected int getLayoutId(Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        return R.layout.activity_map;
    }

    @Override
    protected void initializeToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        titleTv.setText(R.string.address);
    }

    @Override
    protected void initializeActivity() {
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
//        initGeocodeSearch();
        initMapConfig();
    }

    private void initGeocodeSearch(){
        geocoderSearch = new GeocodeSearch(this);
        geocoderSearch.setOnGeocodeSearchListener(this);
    }

    private void getFromLocationAsyn(LatLonPoint lp){
        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(lp, 200,GeocodeSearch.AMAP);
        geocoderSearch.getFromLocationAsyn(query);
    }

    private void getFromLocationName(String name) {
        // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
        GeocodeQuery query = new GeocodeQuery(name, "010");
        geocoderSearch.getFromLocationNameAsyn(query);
    }

    private void closeInputMethod(){
        //得到InputMethodManager的实例
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //如果开启
        if (imm.isActive()) {
            //关闭软键盘，开启方法相同，这个方法是切换开启与关闭状态的
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void initMapConfig(){
        //设置希望展示的地图缩放级别
        CameraUpdate mCameraUpdate = CameraUpdateFactory.zoomTo(17);
        aMap.moveCamera(mCameraUpdate);
        //初始化定位蓝点样式
        myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
//        myLocationStyle.interval(2000);
        myLocationStyle.strokeWidth(1f);
        //设置定位蓝点的Style
        aMap.setMyLocationStyle(myLocationStyle);
        //设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMap.setMyLocationEnabled(true);

        mUiSettings = aMap.getUiSettings();
        mUiSettings.setZoomGesturesEnabled(true);

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        if (screenMarker == null) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.marker_normal));
            screenMarker = aMap.addMarker(new MarkerOptions().zIndex(2).icon(bitmapDescriptor));

            LatLng latLng = new LatLng(latitude,longitude);
            Point screenPosition = aMap.getProjection().toScreenLocation(latLng);
            screenMarker.setPositionByPixels(screenPosition.x, screenPosition.y);
            screenMarker.setClickable(false);
//        screenMarkerJump(aMap, screenMarker);
            screenMarker.setPosition(latLng);

            lp = new LatLonPoint(latLng.latitude, latLng.longitude);
            aMap.setInfoWindowAdapter(new InfoWinAdapter());
            screenMarker.setTitle("位置");
            screenMarker.setSnippet(addressName);
            screenMarker.showInfoWindow();
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(AMapUtil.convertToLatLng(lp), aMap.getCameraPosition().zoom));
        }
    }

    public void screenMarkerJump(AMap aMap, Marker screenMarker) {
        if (screenMarker != null) {
            final LatLng latLng = screenMarker.getPosition();
            Point point = aMap.getProjection().toScreenLocation(latLng);
            point.y -= 50;
            LatLng target = aMap.getProjection().fromScreenLocation(point);
            //使用TranslateAnimation,填写一个需要移动的目标点
            Animation animation = new TranslateAnimation(target);
            animation.setInterpolator(new Interpolator() {
                @Override
                public float getInterpolation(float input) {
                    // 模拟重加速度的interpolator
                    if (input <= 0.5) {
                        return (float) (0.5f - 2 * (0.5 - input) * (0.5 - input));
                    } else {
                        return (float) (0.5f - Math.sqrt((input - 0.5f) * (1.5f - input)));
                    }
                }
            });
            //整个移动所需要的时间
            animation.setDuration(600);
            //设置动画
            screenMarker.setAnimation(animation);
            //开始动画
            screenMarker.startAnimation();
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        if (location != null) {
            Bundle bundle = location.getExtras();
            if (bundle != null) {
                mLocationLatitude = location.getLatitude();
                mLocationLongitude = location.getLongitude();
                if (isFirst) {
                    if (mLocationLatitude > 0 && mLocationLongitude > 0) {
                        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(new LatLng(mLocationLatitude, mLocationLongitude), 17);
                        aMap.moveCamera(cu);
                    }
                    isFirst = false;
                }
                ToastUtil.showShort("定位成功");
            } else {
                ToastUtil.showShort("定位失败，请检查您的定位权限");
            }
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                addressName = result.getRegeocodeAddress().getFormatAddress();
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(AMapUtil.convertToLatLng(lp), aMap.getCameraPosition().zoom));
                screenMarker.setTitle("位置");
                screenMarker.setSnippet(addressName);
                screenMarker.showInfoWindow();
                if(isFirstAdd){
                    double latitude = lp.getLatitude();
                    double longitude = lp.getLongitude();
                    Log.e("TAG",  latitude + "," + longitude);
                    infoEntity.setLocation(latitude + "," + longitude);
                    infoEntity.setLocation_address(addressName);

                    isFirstAdd = false;
                }
            }
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult result, int rCode) {
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getGeocodeAddressList() != null
                    && result.getGeocodeAddressList().size() > 0) {
                GeocodeAddress address = result.getGeocodeAddressList().get(0);
                LatLng latLng = new LatLng(address.getLatLonPoint().getLatitude(), address.getLatLonPoint().getLongitude());
                Point screenPosition = aMap.getProjection().toScreenLocation(latLng);
                screenMarker.setPositionByPixels(screenPosition.x, screenPosition.y);
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(AMapUtil.convertToLatLng(address.getLatLonPoint()), aMap.getCameraPosition().zoom));
                screenMarker.setClickable(false);
                screenMarkerJump(aMap, screenMarker);
            }
        }
    }


    @Override
    public void onGetInputtips(List<Tip> tipList, int rCode) {
//        if (rCode == AMapException.CODE_AMAP_SUCCESS) {// 正确返回
//            List<String> listString = new ArrayList<>();
//            for (int i = 0; i < tipList.size(); i++) {
//                listString.add(tipList.get(i).getName());
//            }
//            ArrayAdapter<String> aAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.route_inputs, listString);
//            mSearchText.setAdapter(aAdapter);
//            aAdapter.notifyDataSetChanged();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                aMap.setOnCameraChangeListener(MapActivity.this);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }
}
