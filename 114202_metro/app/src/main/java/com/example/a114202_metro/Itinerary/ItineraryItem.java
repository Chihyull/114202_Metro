package com.example.a114202_metro.Itinerary;

public class ItineraryItem {
    int itsNo;            // ★ 新增：後端傳回的行程編號
    String title, startDate, endDate, dest;

    public ItineraryItem(int itsNo, String t, String s, String e, String d) {
        this.itsNo = itsNo;
        this.title = t;
        this.startDate = s;
        this.endDate = e;
        this.dest = d;
    }
}
