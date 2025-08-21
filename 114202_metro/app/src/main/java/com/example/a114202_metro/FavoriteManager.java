package com.example.a114202_metro;
//修改中
public class FavoriteManager {
    private static ApiService apiService = ApiClient.getClient().create(ApiService.class);

    public static void addFavorite(int userId, String name, String address, Callback<String> callback) {
        Call<String> call = apiService.addFavorite("add", userId, name, address);
        call.enqueue(callback);
    }

    public static void removeFavorite(int userId, String name, Callback<String> callback) {
        Call<String> call = apiService.removeFavorite("remove", userId, name);
        call.enqueue(callback);
    }

    public static void getFavorites(int userId, Callback<List<Favorite>> callback) {
        Call<List<Favorite>> call = apiService.getFavorites("list", userId);
        call.enqueue(callback);
    }
}

