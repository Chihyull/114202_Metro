package com.example.a114202_metro.Station;

public class StationModel {

    private String StationCode;
    private String NameE;

    public StationModel(String stationCode, String nameE) {
        StationCode = stationCode;
        NameE = nameE;
    }

    public String getStationCode() {
        return StationCode;
    }

    public String getNameE() {
        return NameE;
    }

}
