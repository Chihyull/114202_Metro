package com.example.a114202_metro.Station;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface StationApiService {

    // 獲取捷運站點列表的 API 端點
    @GET("getStations.php")
    Call<List<Station>> getStations();  // 返回一個包含站點資料的列表

}
