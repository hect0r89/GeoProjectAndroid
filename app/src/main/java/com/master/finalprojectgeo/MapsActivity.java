package com.master.finalprojectgeo;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.TreeMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;
import static com.master.finalprojectgeo.R.id.map;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>, LocationListener {

    /**
     * Variable usada para asegurar que no se desconecta el cliente Google si el mapa no ha sido iniciado
     */
    Boolean fromOnMapReady = false;
    /**
     * Etiqueta para identificar las trazas de esta activity
     */
    protected static final String TAG = "MapsActivity";
    /**
     * Atributo Mapa de GoogleMaps que se mostrará en el dispositivo
     */
    private GoogleMap mMap;
    /**
     * Dialog que se mostrará en los procesos de carga
     */
    private ProgressDialog dialogLoading;
    /**
     * Id del usuario y dispositivo que se mandará al servidor
     */
    private String deviceId;
    /**
     * Radio por defecto que se aplica a los Geofence
     */
    private static final int RADIUS_GEOFENCE = 150;
    /**
     * Tiempo de espera por defecto aplicado a los triggered de los Geofences
     */
    private static final int TIME_WAIT_GEOFENCE = 10000;
    /**
     * Constante usada para medir las distancias entre dos puntos
     */
    public static final double R_H = 6372.8; // In kilometers
    /**
     * LocationRequest se utilizan para solicitar un servicio para las actualizaciones de ubicación
     */
    private LocationRequest mLocationRequest;
    /**
     * Variable que almacena la última ubicación
     */
    private Location mLastLocation;
    /**
     * TreeMap donde se almacenan ordenados distancias entre geofence y usuario
     */
    private TreeMap<Double, Geofence> distanceGeofences;
    /**
     * Punto de entrada para el cliente de Google
     */
    protected GoogleApiClient mGoogleApiClient;
    /**
     * Lista de geofences
     */
    protected ArrayList<Geofence> mGeofenceList;
    /**
     * Intent para añadir o eliminar Geofences
     */
    private PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //Se inicializa el cliente de Google
        buildGoogleApiClient();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
        mGeofencePendingIntent = null;
        //Se obtiene el id único del usuario de este dispositivo
        deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        //Se asigna la toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        //Se inicializa el TreeMap vacío
        distanceGeofences = new TreeMap<>();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (!fromOnMapReady) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Se infla el menú que aparecerá en la toolbar
        getMenuInflater().inflate(R.menu.menu_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                //Si el botón pulsado en el menú es el de actualizar se llama al método actualizar
                checkForUpdates();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Listener que se usa para detectar una pulsaxión larga en el mapa y que añade el marcador y realiza la petición
     */
    GoogleMap.OnMapLongClickListener mapLongClickListener = new GoogleMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(final LatLng latLng) {
            //Se crea un custom dialog con un input text que permite introducir el mensaje
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            LayoutInflater inflater = MapsActivity.this.getLayoutInflater();
            final View v = inflater.inflate(R.layout.dialog_geo_create, null);
            builder.setView(v)
                    //En caso de confirmar la creación
                    .setPositiveButton("Crear", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Se muestra una barra de carga
                            dialogLoading = ProgressDialog.show(MapsActivity.this, "", "Cargando...");
                            EditText message = (EditText) v.findViewById(R.id.editTextMensaje);
                            //Se crea el objeto punto que será enviado al servidor
                            PointGeo point = new PointGeo(latLng.latitude, latLng.longitude, RADIUS_GEOFENCE, message.getText().toString(), deviceId);
                            //Se hace uso del servicio Retrofit para conectarnos
                            ServiceRetrofit contactService = ServiceRetrofit.retrofit.create(ServiceRetrofit.class);
                            //Se realiza una petición POST pasando el objeto
                            Call<PointGeo> call = contactService.postPoint(point);
                            call.enqueue(new Callback<PointGeo>() {
                                @Override
                                public void onResponse(Call<PointGeo> call, Response<PointGeo> response) {
                                    //En caso de respuesta afirmativa del servidor se actualizan los datos
                                    dialogLoading.dismiss();
                                    checkForUpdates();
                                }

                                @Override
                                public void onFailure(Call<PointGeo> call, Throwable t) {
                                    dialogLoading.dismiss();
                                }
                            });
                        }
                    })
                    //En caso de cancelar la creación se cierra el dialog
                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

            builder.show();


        }
    };

    /**
     * Este callback es llamado cuando el mapa está listo para usarse
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        fromOnMapReady = true;
        //Solicitamos permisos de ubicación los únicos necesarios
        Dexter.withActivity(this)
                .withPermission(ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mMap.setMyLocationEnabled(true);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {/* ... */}

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

        //Se obtiene el tamaño de la toolbar en tiempo de ejecución
        final TypedArray styledAttributes = getApplicationContext().getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int mActionBarSize = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        //Se añade un padding top al mapa para que no oculte la toolbar los botones del mapa
        mMap.setPadding(0, mActionBarSize, 0, 0);
        //Se asigna el listener explicado antes para largas pulsaciones
        mMap.setOnMapLongClickListener(mapLongClickListener);
        //Se asigna un listener en caso de que se pulse un marcador, para mostrar un dialogo de detalle
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.hideInfoWindow();
                String snippet = marker.getSnippet();
                dialogLoading = ProgressDialog.show(MapsActivity.this, "", "Cargando...");
                //Se realiza una petición para obtener el detalle del punto clickado
                final ServiceRetrofit pointService = ServiceRetrofit.retrofit.create(ServiceRetrofit.class);
                Call<PointGeo> call = pointService.getPoint(Integer.parseInt(snippet));
                call.enqueue(new Callback<PointGeo>() {
                    @Override
                    public void onResponse(Call<PointGeo> call, Response<PointGeo> response) {
                        dialogLoading.dismiss();
                        //En caso de respuesta afirmativa se muestra el detalle
                        showDetail(response.body());
                    }

                    @Override
                    public void onFailure(Call<PointGeo> call, Throwable t) {
                        dialogLoading.dismiss();
                    }
                });
                return true;
            }
        });
    }

    /**
     * Método que muestra un dialogo de detalle con la información de un punto y que permite eliminarlo si es posible
     *
     * @param point objeto PointGeo a mostrar
     */
    private void showDetail(final PointGeo point) {
        //Se crea otro custom dialog y sus elementos para rellenarlos con los datos del punto
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        LayoutInflater inflater = MapsActivity.this.getLayoutInflater();
        final View v = inflater.inflate(R.layout.dialog_geo_detail, null);
        TextView message = (TextView) v.findViewById(R.id.txtTitleDetail);
        message.setText(point.getMessage());
        TextView lat = (TextView) v.findViewById(R.id.txtLatDetail);
        lat.setText(String.valueOf(point.getLat()));
        TextView lon = (TextView) v.findViewById(R.id.txtLonDetail);
        lon.setText(String.valueOf(point.getLon()));
        builder.setView(v)
                //En caso de respuesta afirmativa  se cierra el cuadro
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
            //En caso de respuesta negativa se realiza una petición para eliminar el punto
        }).setNegativeButton("Eliminar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Se realiza una petición delete sobre la id enviada
                final ServiceRetrofit pointService = ServiceRetrofit.retrofit.create(ServiceRetrofit.class);
                Call<Response<Void>> call = pointService.deletePoint(point.getId());
                call.enqueue(new Callback<Response<Void>>() {
                    @Override
                    public void onResponse(Call<Response<Void>> call, Response<Response<Void>> response) {
                        dialogLoading.dismiss();
                        //En caso de respuesta afirmativa se actualizan los datos
                        checkForUpdates();
                    }
                    @Override
                    public void onFailure(Call<Response<Void>> call, Throwable t) {
                        dialogLoading.dismiss();
                    }
                });
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        //Se comprueba si el usuario que ha hecho click es el mismo que creó el marcador, si lo es puede eliminarlo
        if (!point.getDevice_id().equals(deviceId)) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setEnabled(false);
        }
    }

    /**
     * Método que construye el cliente de Google
     */
    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


    }

    /**
     * Método en el que se asigna el intent que reaccionará a los eventos del Geofence
     * @return PendingIntent
     */
    private PendingIntent getGeofencePendingIntent() {
        // Si ya existe se reutiliza
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        //Se hace un intent a la clase en la que se controlarán las transiciones
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Método en el que se configuran los eventos a los que reacciona el geofence y en el que se añaden
     * @return GeofencingRequest
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        //El servicio reaccionará en este caso a los eventos de entrada y de mantenerse en un geofence
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL);

        // Se añade la lista de geofences
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    /**
     * Método que realiza las peticiones Get al servidor y actualiza todos los datos de la aplicación
     */
    private void checkForUpdates() {
        dialogLoading = ProgressDialog.show(MapsActivity.this, "", "Cargando...");
        //Se limpia el mapa de marcadores y geofences
        mMap.clear();
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                getGeofencePendingIntent()
        ).setResultCallback(this);

        //Se realiza una petición para obtener todos los puntos almacenados en el servidor
        ServiceRetrofit contactService = ServiceRetrofit.retrofit.create(ServiceRetrofit.class);
        Call<ArrayList<PointGeo>> call = contactService.getPoints();
        call.enqueue(new Callback<ArrayList<PointGeo>>() {
            @Override
            public void onResponse(Call<ArrayList<PointGeo>> call, Response<ArrayList<PointGeo>> response) {
                if (response.body() != null) {
                    //En caso de respuesta positiva recorremos todos los puntos recibidos
                    for (PointGeo point : response.body()) {
                        //Se añade el marcador al mapa, en el snippet se guarda la id para poder obtener el detalle cuando se pulse el marcador
                        mMap.addMarker(new MarkerOptions().position(new LatLng(point.getLat(), point.getLon())).title(point.getMessage()).snippet(String.valueOf(point.getId())));
                        //Se construye el geofence
                        Geofence geofence = new Geofence.Builder()
                                //Se asigna el id
                                .setRequestId(String.valueOf(point.getId()))
                                // Se asignan sus puntos y su radio
                                .setCircularRegion(
                                        point.getLat(),
                                        point.getLon(),
                                        point.getRadius()
                                )
                                //Se asigna la duración, en este caso nunca expirará
                                .setExpirationDuration(NEVER_EXPIRE)
                                //Se asocian las transiciones que serán lanzadas
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)
                                .setLoiteringDelay(TIME_WAIT_GEOFENCE)
                                .build();
                        // Se guarda en un TreeMap ordenado la distancia entre la localización y el punto como clave y el geofence como valor
                        if(mLastLocation!=null){
                            distanceGeofences.put(haversine(mLastLocation.getLatitude(), mLastLocation.getLongitude(), point.getLat(), point.getLon()) * 1000, geofence);
                        }

                    }
                }
                if (distanceGeofences.size() > 0) {
                    //Se guarda en el ArrayList los valores del TreeMap ya ordenados
                    mGeofenceList = new ArrayList<>(distanceGeofences.values());
                    //En el caso que haya mas de 100 se seleccionan solo los 100 primeros, los más cercanos
                    if (mGeofenceList.size() > 100) {
                        mGeofenceList = new ArrayList<>(mGeofenceList.subList(0, 100));
                    }
                    try {
                        //Se añaden los Geofences
                        LocationServices.GeofencingApi.addGeofences(
                                mGoogleApiClient,
                                getGeofencingRequest(),
                                getGeofencePendingIntent()
                        ).setResultCallback(MapsActivity.this);
                    } catch (SecurityException securityException) {

                    }
                }
                dialogLoading.dismiss();
            }
            @Override
            public void onFailure(Call<ArrayList<PointGeo>> call, Throwable t) {
                dialogLoading.dismiss();
            }
        });
    }

    /**
     * Método que devuelve la distancia en Km entre dos puntos
     * @param lat1 Latitud del punto A
     * @param lon1 Longitud del punto A
     * @param lat2 Latitud del punto B
     * @param lon2 Longitud del punto B
     * @return Distancia en km
     */
    public static Double haversine(Double lat1, Double lon1, Double lat2, Double lon2) {
        Double dLat = Math.toRadians(lat2 - lat1);
        Double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        Double c = 2 * Math.asin(Math.sqrt(a));
        return R_H * c;
    }


    /**
     * Se ejecuta si el cliente de google se ha conectado
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        checkForUpdates();
        //Se configura la petición de localización al usuario
        Dexter.withActivity(this)
                .withPermission(ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mLocationRequest = new LocationRequest();
                        mLocationRequest.setInterval(1000);
                        mLocationRequest.setFastestInterval(1000);
                        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, MapsActivity.this);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {/* ... */}

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    @Override
    public void onConnectionSuspended(int i) {
        // Se ha perdido la conexión con el cliente
        Log.i(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //La conexión ha fallado
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {


        } else {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //Se actualiza la ultima ubicación del usuario
        mLastLocation = location;
    }
}
