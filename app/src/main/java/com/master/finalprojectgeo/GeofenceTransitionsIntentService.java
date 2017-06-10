package com.master.finalprojectgeo;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class GeofenceTransitionsIntentService extends IntentService {

    /**
     * Constante para mostrar los logs
     */
    protected static final String TAG = "GeofenceTransitionsIS";
    private ArrayList<PointGeo> points;


    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        points = new ArrayList<>();
    }

    /**
     * Handles incoming intents.
     *
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Se obtieene el tipo de transición
        final int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Si la transición es la requerida, mantenerse en el geofence el tiempo estimado
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            Log.d(TAG, "Transición del geofence comenzada");
            // Se obtienen todos los geofences que han sido lanzados
            final List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            for (Geofence geofence : triggeringGeofences) {
                // Por cada geofence se realiza una petición obteniendo el detalle del punto
                final ServiceRetrofit pointService = ServiceRetrofit.retrofit.create(ServiceRetrofit.class);
                Call<PointGeo> call = pointService.getPoint(Integer.parseInt(geofence.getRequestId()));
                call.enqueue(new Callback<PointGeo>() {
                    @Override
                    public void onResponse(Call<PointGeo> call, Response<PointGeo> response) {
                        if (response.body() != null) {
                            Log.d("OK:", "Petición GET realizada correctamente");
                            points.add(new PointGeo(response.body().getLat(), response.body().getLon(), response.body().getRadius(), response.body().getMessage(), response.body().getDevice_id()));
                            //Una vez se ha recibido el punto desde el backend se envía la notificación al usuario
                            sendNotification(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<PointGeo> call, Throwable t) {
                        Log.d("Error:", "Fallo al realizar la petición");
                    }
                });
            }


        } else {
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    /**
     * Método que gestiona el envío de notificaciones al usuario
     *
     * @param point Objeto Geopoint
     */
    private void sendNotification(PointGeo point) {
        // Se crea el intent en la activity principal
        Intent notificationIntent = new Intent(getApplicationContext(), MapsActivity.class);

        // Se crea un stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Se añade la activity principal al stack como padre
        stackBuilder.addParentStack(MapsActivity.class);

        // Se añade el pending intent al stack
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Se definen los settings de la notificación
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle("Dato de interés")
                .setContentText(point.getMessage())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(point.getMessage()))
                .setContentIntent(notificationPendingIntent);

        // Si el usuario toca se cierra la notificación
        builder.setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Se lanza la notificación
        mNotificationManager.notify(point.getId(), builder.build());
        Log.d(TAG, "Notificación lanzada correctamente");
    }
}