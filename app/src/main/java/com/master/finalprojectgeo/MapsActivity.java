package com.master.finalprojectgeo;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    Boolean fromOnMapReady = false;
    protected static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private ProgressDialog dialogLoading;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * The list of geofences used in this sample.
     */
    protected ArrayList<Geofence> mGeofenceList;

    /**
     * Used to keep track of whether geofences were added.
     */
    private boolean mGeofencesAdded;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;
    SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        buildGoogleApiClient();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<>();
        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

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

    GoogleMap.OnMapLongClickListener mapLongClickListener = new GoogleMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(final LatLng latLng) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            LayoutInflater inflater = MapsActivity.this.getLayoutInflater();
            final View v = inflater.inflate(R.layout.dialog_geo_create, null);
            builder.setView(v)
                    .setPositiveButton("Crear", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //TODO Enviaremos los datos al servidor
                            dialogLoading = ProgressDialog.show(MapsActivity.this, "", "Cargando...");
                            EditText message = (EditText) v.findViewById(R.id.editTextMensaje);
                            PointGeo point = new PointGeo(latLng.latitude, latLng.longitude, 5, message.getText().toString());
                            ServiceRetrofit contactService = ServiceRetrofit.retrofit.create(ServiceRetrofit.class);
                            Call<PointGeo> call = contactService.postPoint(point);
                            call.enqueue(new Callback<PointGeo>() {

                                @Override
                                public void onResponse(Call<PointGeo> call, Response<PointGeo> response) {
                                    dialogLoading.dismiss();
                                }

                                @Override
                                public void onFailure(Call<PointGeo> call, Throwable t) {
                                    dialogLoading.dismiss();
                                }
                            });
                            mMap.addMarker(new MarkerOptions().position(latLng).title(message.getText().toString()));
                        }
                    })
                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            builder.show();


        }
    };

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        fromOnMapReady = true;
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
        // Kick off the request to build GoogleApiClient.
        mMap.setOnMapLongClickListener(mapLongClickListener);

    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    private void checkForUpdates() {
        dialogLoading = ProgressDialog.show(MapsActivity.this, "", "Cargando...");
        ServiceRetrofit contactService = ServiceRetrofit.retrofit.create(ServiceRetrofit.class);
        Call<ArrayList<PointGeo>> call = contactService.getPoints();
        call.enqueue(new Callback<ArrayList<PointGeo>>() {

            @Override
            public void onResponse(Call<ArrayList<PointGeo>> call, Response<ArrayList<PointGeo>> response) {
                if (response.body() != null) {
                    for (PointGeo point : response.body()) {
                        mMap.addMarker(new MarkerOptions().position(new LatLng(point.getLat(), point.getLon())).title(point.getMessage()));
                        Geofence geofence = new Geofence.Builder()
                                // Set the request ID of the geofence. This is a string to identify this
                                // geofence.
                                .setRequestId(String.valueOf(point.getId()))

                                // Set the circular region of this geofence.
                                .setCircularRegion(
                                        point.getLat(),
                                        point.getLon(),
                                        point.getRadius()
                                )

                                // Set the expiration duration of the geofence. This geofence gets automatically
                                // removed after this period of time.
                                .setExpirationDuration(NEVER_EXPIRE)

                                // Set the transition types of interest. Alerts are only generated for these
                                // transition. We track entry and exit transitions in this sample.
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                        Geofence.GEOFENCE_TRANSITION_EXIT |
                                        Geofence.GEOFENCE_TRANSITION_DWELL)
                                .setLoiteringDelay(1000)
                                // Create the geofence.
                                .build();
                        mGeofenceList.add(geofence);
                        CircleOptions circleOptions = new CircleOptions()
                                .strokeColor(Color.BLACK) //Outer black border
                                .fillColor(Color.TRANSPARENT) //inside of the geofence will be transparent, change to whatever color you prefer like 0x88ff0000 for mid-transparent red
                                .center(new LatLng(point.getLat(), point.getLon())) // the LatLng Object of your geofence location
                                .radius(point.getRadius()); // The radius (in meters) of your geofence

// Get back the mutable Circle
                        Circle circle = mMap.addCircle(circleOptions);
                    }


                }

                if (mGeofenceList.size() > 0) {
                    Log.d(TAG, "Añadidos los geofences");
                    try {
                        LocationServices.GeofencingApi.addGeofences(
                                mGoogleApiClient,
                                // The GeofenceRequest object.
                                getGeofencingRequest(),
                                // A pending intent that that is reused when calling removeGeofences(). This
                                // pending intent is used to generate an intent when a matched geofence
                                // transition is observed.
                                getGeofencePendingIntent()
                        ).setResultCallback(MapsActivity.this); // Result processed in onResult().
                    } catch (SecurityException securityException) {
                        // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.

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
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");
        checkForUpdates();

    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason.
        Log.i(TAG, "Connection suspended");
        // onConnected() will be called again automatically when the service reconnects
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;


            // Update the UI. Adding geofences enables the Remove Geofences button, and removing
            // geofences enables the Add Geofences button.
            //setButtonsEnabledState();


        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }
}
