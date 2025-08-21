package com.example.a114202_metro;
//修改中
public class Favorite {
    private String name;
    private String address;
    private boolean isLiked;

    public Favorite(String name, String address, boolean isLiked) {
        this.name = name;
        this.address = address;
        this.isLiked = isLiked;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }
}
