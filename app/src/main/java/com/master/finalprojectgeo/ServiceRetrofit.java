package com.master.finalprojectgeo;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Created by Hector on 04/06/2017.
 */

interface ServiceRetrofit {

    @GET("points/")
    Call<ArrayList<PointGeo>> getPoints();

    @POST("points/")
    Call<PointGeo> postPoint(@Body PointGeo point);

    @GET("points/{id}/")
    Call<PointGeo> getPoint(@Path("id") int pointId);

    @PUT("points/{id}/")
    Call<PointGeo> putPoint(@Path("id") long contactId, @Body PointGeo c);

    @DELETE("points/{id}/")
    Call<Response<Void>> deletePoint(@Path("id") long contactId);

    public static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://165.227.79.216:80/api/1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
}
