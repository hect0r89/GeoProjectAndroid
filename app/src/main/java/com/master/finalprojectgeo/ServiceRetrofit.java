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
import retrofit2.http.Path;

/**
 * Interfaz que permite la conexión con el backend
 */
interface ServiceRetrofit {

    /**
     * Método que realiza la petición GET para obtener los puntos
     * @return ArrayList de PointGeo
     */
    @GET("points/")
    Call<ArrayList<PointGeo>> getPoints();

    /**
     * Método que realiza la petición POST para crear un punto
     * @param point GeoPoint
     * @return PointGeo
     */
    @POST("points/")
    Call<PointGeo> postPoint(@Body PointGeo point);

    /**
     * Método que realiza la petición GET para obtener un punto en concreto
     * @param pointId id del punto
     * @return PointGeo
     */
    @GET("points/{id}/")
    Call<PointGeo> getPoint(@Path("id") int pointId);

    /**
     * Método que realiza la petición DELETE  para borrar un punto
     * @param pointId id del punto
     * @return Response
     */
    @DELETE("points/{id}/")
    Call<Response<Void>> deletePoint(@Path("id") int pointId);

    public static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://165.227.79.216:80/api/1.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
}
