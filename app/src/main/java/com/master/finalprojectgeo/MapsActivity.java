package com.master.finalprojectgeo;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    PermissionListener dialogPermissionListener =
            DialogOnDeniedPermissionListener.Builder
                    .withContext(getApplicationContext())
                    .withTitle("Camera permission")
                    .withMessage("Camera permission is needed to take pictures of your cat")
                    .withButtonText(android.R.string.ok)
                    .build();

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
        Dexter.withActivity(this)
                .withPermission(ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override public void onPermissionGranted(PermissionGrantedResponse response) {
                        mMap.setMyLocationEnabled(true);
                    }
                    @Override public void onPermissionDenied(PermissionDeniedResponse response) {/* ... */}
                    @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) { token.continuePermissionRequest();}
                }).check();
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
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
                                dialog = ProgressDialog.show(MapsActivity.this, "", "Cargando...");
                                EditText message = (EditText) v.findViewById(R.id.editTextMensaje);
                                PointGeo point = new PointGeo(latLng.latitude, latLng.longitude, 5, message.getText().toString());
                                ServiceRetrofit contactService = ServiceRetrofit.retrofit.create(ServiceRetrofit.class);
                                Call<PointGeo> call = contactService.postPoint(point);
                                final DialogInterface finalDialog = dialog;
                                call.enqueue(new Callback<PointGeo>() {

                                    @Override
                                    public void onResponse(Call<PointGeo> call, Response<PointGeo> response) {
                                        finalDialog.dismiss();
                                    }

                                    @Override
                                    public void onFailure(Call<PointGeo> call, Throwable t) {
                                        finalDialog.dismiss();
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
        });

    }
}
