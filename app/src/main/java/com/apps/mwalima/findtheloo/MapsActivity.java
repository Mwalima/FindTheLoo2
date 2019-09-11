package com.apps.mwalima.findtheloo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {


    private static final double EARTH_RADIUS = 6371000;
    private LatLng latLng;
    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Location lastlocation;
    private List<Address> addressList;
    private LocationListener mListener;
    private double lat;
    private double lng;
    private Marker currentLocationmMarker;
    public static final int REQUEST_LOCATION_CODE = 99;
    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationClient;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager ()
                    .findFragmentById (R.id.map);
            mapFragment.getMapAsync (this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode)
        {
            case REQUEST_LOCATION_CODE:
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) !=  PackageManager.PERMISSION_GRANTED)
                    {
                        if(client == null)
                        {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else
                {
                    Toast.makeText(this,"Permission Denied" , Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    public void onResume() {
        super.onResume ();
        if (client != null && mFusedLocationClient != null) {
            requestLocationUpdates();
        } else {
            buildGoogleApiClient();
        }
    }
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
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }else{
                checkLocationPermission ();
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        client.connect();
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations ();
            Object dataTransfer[] = new Object[2];
            GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();


            if (locationList.size () > 0) {
                //The last location in the list is the newest
                Location location = locationList.get (locationList.size () - 1);
                Log.i ("MapsActivity", "Location: " + location.getLatitude () + " " + location.getLongitude ());
                lastlocation = location;
                if (currentLocationmMarker != null) {
                    currentLocationmMarker.remove ();
                }

                //Place current location marker
                LatLng latLng = new LatLng (location.getLatitude (), location.getLongitude ());
                MarkerOptions markerOptions = new MarkerOptions ();
                markerOptions.position (latLng);
                markerOptions.title ("Current Position");
                markerOptions.icon (BitmapDescriptorFactory.defaultMarker (BitmapDescriptorFactory.HUE_RED));
                currentLocationmMarker = mMap.addMarker (markerOptions);

                mMap.clear ();
                mMap.addCircle (
                        new CircleOptions ()
                                .center (latLng)
                                .radius (500.0)
                                .strokeWidth (3f)
                                .strokeColor (Color.RED)
                                .fillColor (Color.argb (70,150,50,50))
                );
                String all = "cafe&restaurant&mcdonalds&supermarket&public+toilet";
                String url = getUrl (location.getLatitude (), location.getLongitude (), all);
                dataTransfer[0] = mMap;
                dataTransfer[1] = url;
                getNearbyPlacesData.execute (dataTransfer);
                Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde mogelijkheden", Toast.LENGTH_SHORT).show ();

                mMap.moveCamera (CameraUpdateFactory.newLatLngZoom (latLng, 15));
            }
        }
    };


    @Override
    public void onLocationChanged(Location location) {
      lastlocation=location;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


    public void onClick(View v)
    {
        Object dataTransfer[] = new Object[2];
        GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
        EditText tf_location =  findViewById(R.id.onFocus);
        String Searchlocation = tf_location.getText().toString();

        switch(v.getId())
        {
            case R.id.B_search:
                if(!Searchlocation.equals(""))
                {
                    Geocoder geocoder = new Geocoder(this);

                    try {
                        addressList = geocoder.getFromLocationName(Searchlocation, 1);

                        for(int i = 0; i < addressList.size (); i++) {

                            latLng = new LatLng (addressList.get (0).getLatitude (), addressList.get (0).getLongitude ());
                            lat = addressList.get (0).getLatitude ();
                            lng = addressList.get (0).getLongitude ();
                            MarkerOptions markerOptions = new MarkerOptions ();
                            markerOptions.position (latLng);
                            markerOptions.title (Searchlocation);
                            mMap.addMarker (markerOptions);
                        }

                        mMap.clear ();
                        mMap.addCircle (
                                new CircleOptions ()
                                        .center (latLng)
                                        .radius (500.0)
                                        .strokeWidth (3f)
                                        .strokeColor (Color.RED)
                                        .fillColor (Color.argb (70,150,50,50))
                        );
                        String all = "cafe&restaurant&mcdonalds&supermarket&public+toilet";
                        String url = getUrl (lat, lng, all);
                        dataTransfer[0] = mMap;
                        dataTransfer[1] = url;
                        getNearbyPlacesData.execute (dataTransfer);
                        Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde mogelijkheden in "+ Searchlocation, Toast.LENGTH_SHORT).show ();

                        mMap.moveCamera (CameraUpdateFactory.newLatLngZoom (latLng, 14.0f));
                        mMap.animateCamera (CameraUpdateFactory.zoomTo (13), 500, null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.B_supermarkten:
                if(!Searchlocation.isEmpty ()) {
                    mMap.clear ();
                    String supermarkt = "supermarket";
                    String url = getUrl (lat, lng, supermarkt);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute (dataTransfer);
                    Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde supermarkten in "+ Searchlocation, Toast.LENGTH_SHORT).show ();
                }
                if(Searchlocation.isEmpty ()) {
                    mMap.clear ();
                    String supermarkt = "supermarket";
                    String url = getUrl (lastlocation.getLatitude (), lastlocation.getLongitude (), supermarkt);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute (dataTransfer);
                    Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde supermarkten", Toast.LENGTH_SHORT).show ();
                }
                break;
            case R.id.B_cafe:
                if(!Searchlocation.isEmpty ()) {
                    mMap.clear ();
                    String cafe = "cafe";
                    String url = getUrl (lat, lng, cafe);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute (dataTransfer);
                    Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde cafe in "+ Searchlocation, Toast.LENGTH_SHORT).show ();
                }
                if(Searchlocation.isEmpty ()) {
                    mMap.clear ();
                    String cafe = "cafe";
                    String url = getUrl (lastlocation.getLatitude (), lastlocation.getLongitude (), cafe);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute (dataTransfer);
                    Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde cafe`s", Toast.LENGTH_SHORT).show ();
                }
                break;
            case R.id.B_restaurants:
                if(!Searchlocation.isEmpty ()) {
                    mMap.clear ();
                    String restaurant = "restaurant";
                    String url = getUrl (lat, lng, restaurant);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute (dataTransfer);
                    Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde restaurant in "+ Searchlocation, Toast.LENGTH_SHORT).show ();
                }
                if(Searchlocation.isEmpty ()) {
                    mMap.clear();
                    String restaurant = "restaurant";
                    String url = getUrl(lastlocation.getLatitude(), lastlocation.getLongitude(),restaurant);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute(dataTransfer);
                    Toast.makeText(MapsActivity.this, "Toon dichtsbijzijnde restaurants", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.B_mcdonalds:
                if(!Searchlocation.isEmpty ()) {
                    mMap.clear ();
                    String mcdonalds = "mcdonalds";
                    String url = getUrl (lat, lng, mcdonalds);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute (dataTransfer);
                    Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde mcdonalds in "+ Searchlocation, Toast.LENGTH_SHORT).show ();
                }
                if(Searchlocation.isEmpty ()) {
                    mMap.clear ();
                    String mcdonalds = "mcdonalds";
                    String url = getUrl (lastlocation.getLatitude (), lastlocation.getLongitude (), mcdonalds);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute (dataTransfer);
                    Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde mcdonalds", Toast.LENGTH_SHORT).show ();
                }
                break;
            case R.id.B_loo:
                if(!Searchlocation.isEmpty ()) {
                    mMap.clear ();
                    String loo = "public toilet";
                    String url = getUrl (lat, lng, loo);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute (dataTransfer);
                    Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde public toilet in "+ Searchlocation, Toast.LENGTH_SHORT).show ();
                }
                if(Searchlocation.isEmpty ()) {
                    mMap.clear ();
                    String loo = "public toilet";
                    String url = getUrl (lastlocation.getLatitude (), lastlocation.getLongitude (), loo);
                    dataTransfer[0] = mMap;
                    dataTransfer[1] = url;
                    getNearbyPlacesData.execute (dataTransfer);
                    Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde public toilets", Toast.LENGTH_SHORT).show ();
                }
                break;
        }
    }

    private String getUrl(double latitude , double longitude , String nearbyPlace)
    {
        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location=" + latitude + "," + longitude);
        googlePlaceUrl.append("&radius="+1000);
        googlePlaceUrl.append("&keyword="+ nearbyPlace);
        googlePlaceUrl.append ("&opennow");
        googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&key="+"AIzaSyBxo_Umr5453x5ij04DVw-euVKNHxSEWPc");

        Log.d("MapsActivity", "url = "+googlePlaceUrl.toString());
        return googlePlaceUrl.toString();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle){
        requestLocationUpdates();
    }

    public void requestLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(120000);
        locationRequest.setFastestInterval(120000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
    }


    public boolean checkLocationPermission()
    {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED )
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
            }
            else
            {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
            }
            return false;
        }
        else
            return true;
    }


    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

}