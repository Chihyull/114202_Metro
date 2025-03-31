package com.example.a114202_metro.Station;

public class StationModel {

    private String StationCode;
    private String NameE;
    private String Line;  // 新增欄位

    public StationModel(String stationCode, String nameE, String line) {
        StationCode = stationCode;
        NameE = nameE;
        Line = line;
    }

    public String getStationCode() {
        return StationCode;
    }

    public String getNameE() {
        return NameE;
    }

    public String getLine() {
        return Line;
    }
}

