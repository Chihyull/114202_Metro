package com.example.a114202_metro.Station;

public class StationModel {
    private String StationCode;
    private String NameE;
    private String Line;       // 線名
    private String LineCode;   // 線代碼

    public StationModel(String stationCode, String nameE, String line, String lineCode) {
        StationCode = stationCode;
        NameE = nameE;
        Line = line;
        LineCode = lineCode;
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

    public String getLineCode() {
        return LineCode;
    }
}

