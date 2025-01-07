package com.example.socialapp.Config;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitInstance {
    private static Retrofit retrofit;
    private static final String BASE_URL = "http://locahost:port/";
    private static final String WEB_URL = "ws://locahost:port/";

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())  // Gson을 사용한 JSON 변환기
                    .build();
        }
        return retrofit;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }   // 서버 연결 시 url
    public static String getWebUrl() { return WEB_URL; }    // initsocket url
}
