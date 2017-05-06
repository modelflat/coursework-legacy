package com.github.modelflat.coursework2.networking;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Created on 06.05.2017.
 */
public class Forismatic {

    public interface ForismaticService {
        @POST("/api/1.0/")
        Call<ResponseBody> getQuote(@Query("method") String method,
                                    @Query("lang") String lang,
                                    @Query("format") String text);
    }

    private static ForismaticService instance = null;

    private static ForismaticService createService() {
        return new Retrofit.Builder().baseUrl("http://api.forismatic.com").build()
                .create(ForismaticService.class);
    }

    public static CompletableFuture<String> getQuote(String lang, String defaultString) {
        return CompletableFuture.supplyAsync(() -> {
            if (instance == null) {
                instance = createService();
            }
            try {
                return instance.getQuote("getQuote", lang, "text").execute().body().string();
            } catch (IOException e) {
                return defaultString;
            }
        });
    }

}
