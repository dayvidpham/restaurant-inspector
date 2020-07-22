package ca.cmpt276.project.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Calendar;

import ca.cmpt276.project.R;
import ca.cmpt276.project.model.Restaurant;
import ca.cmpt276.project.model.RestaurantManager;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String SHARED_PREFS = "sharedPrefs";
    public static final long DEFAULT_DATE = 0;
    private static final long TWENTY_H_IN_MS = 20 * 60 * 60 * 1000;
    private static  final String LAST_UPDATE = "last_update_long";
    //IF there is no exact date there, it is impossible to compare with the server,
    //which date was the last modified date on the server and the app.
    private static  String restaurantlastModifiedDate = "2020-07-01T00:00";
    private static final String  inspectionslastModifiedDate = "2020-07-01T00:00";
    private static final String REST_LAST_MODIFIED_DATE = "rest_last_modified_date";
    private static final String INSP_LAST_MODIFIED_DATE = "insp_last_modified_date";
    private static final String REST_API_URL = "http://data.surrey.ca/api/3/action/package_show?id=restaurants";
    private static final String INSP_API_URL = "http://data.surrey.ca/api/3/action/package_show?id=fraser-health-restaurant-inspection-reports";
    private boolean isUpdated = false;

    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationClient;

    private GoogleMap mMap;
    private Location currentLocation;
    private List<Marker> markers = new ArrayList<>();
    private ClusterManager<Cluster> mClusterManager;
    MarkerClusterRenderer mRenderer;
    LocationManager locationManager;

    RestaurantManager manager;
    int sum;
    UpdateTask updateTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        manager = RestaurantManager.getInstance(getApplicationContext());
        locationManager = (LocationManager) this.getSystemService(MapActivity.LOCATION_SERVICE);

        getLocationPermission();

        // tap peg to pop up name, address and hazard level

        // tap again to goto restaurant's full info page
        try {
            updateRestaurant();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

//        new MapActivity.JSONTask().execute(REST_API_URL);
    }

//




    private void updateRestaurant() throws ParseException, IOException {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        long lastUpdate = sharedPreferences.getLong(LAST_UPDATE, DEFAULT_DATE);
        long currDateLong = Calendar.getInstance().getTimeInMillis();
        String restaurantDate = sharedPreferences.getString(REST_LAST_MODIFIED_DATE,restaurantlastModifiedDate);
        String inspectionsDate = sharedPreferences.getString(INSP_LAST_MODIFIED_DATE, inspectionslastModifiedDate);

        if(currDateLong - lastUpdate < TWENTY_H_IN_MS) {
            return;
        }
        else {
            LocalDateTime restDate = LocalDateTime.parse(restaurantDate);
            //function to check if there is new data


        }

    }

    private void createAskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        builder.setTitle(R.string.update_available_text)
                .setMessage(R.string.update_now_text)
                .setPositiveButton(R.string.yes_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Toast.makeText(MapActivity.this, "onCLick" , Toast.LENGTH_SHORT).show();
                        updateTask = new UpdateTask();
                        updateTask.execute();
                    }
                })
                .setNegativeButton(R.string.no_text, null)
                .setCancelable(false)
                .show();

    }

    private void createUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        LayoutInflater inflater = LayoutInflater.from(MapActivity.this);
        View view = inflater.inflate(R.layout.wait_dialog, null);
        builder.setView(view)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .show();

    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            currentLocation = location;

            moveCamera(new LatLng(location.getLatitude(),location.getLongitude()), DEFAULT_ZOOM);
        }
    };

    // display pegs showing the location of each restaurant with hazard icons
    private void geoLocate() {
        setUpClusterer();
        Cluster offsetItem;

        if (manager.size() > 0) {
            for (Restaurant res : manager.restaurants()) {
                if (!res.inspections.isEmpty()) {
                    offsetItem= new Cluster(res.latitude, res.longitude, res.name, res.address, res.inspections.get(0).hazardRating.toString());
                } else {
                    offsetItem = new Cluster(res.latitude, res.longitude, res.name, res.address, "null");
                }
                //mRenderer = new DefaultClusterRenderer(MapActivity.this, mMap, mClusterManager);
                //mClusterManager.setRenderer(mRenderer);
                mClusterManager.addItem(offsetItem);
                mClusterManager.setOnClusterItemInfoWindowClickListener(new ClusterManager.OnClusterItemInfoWindowClickListener<Cluster>() {
                    @Override
                    public void onClusterItemInfoWindowClick(Cluster item) {
                        for(Restaurant res:manager.restaurants()){
                            if (item.getPosition().longitude == res.longitude) {
                                Intent intent = new Intent(MapActivity.this, RestaurantActivity.class);
                                intent.putExtra("tracking number", res.trackingNumber);
                                startActivityForResult(intent, 1);
                                break;
                            }
                        }
                    }
                });
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                double longitude = data.getDoubleExtra("longitude", 0);
                for(Marker m: markers){
                    if(m.getPosition().longitude == longitude){
                        m.showInfoWindow();
                        moveCamera(m.getPosition(),DEFAULT_ZOOM);
                        break;
                    }
                }
            }
        }
    }

    private void setUpClusterer() {

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<Cluster>(this, mMap);
        mRenderer = new MarkerClusterRenderer(this, mMap, mClusterManager);
        mClusterManager.setRenderer(mRenderer);
        mMap.setOnCameraIdleListener(mClusterManager);

    }

    // get user location and center on current location
    private void getDeviceLocation() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionsGranted) {

                Task location = mFusedLocationClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            currentLocation = (Location) task.getResult();
                            if(currentLocation!=null) {
                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                        DEFAULT_ZOOM);
                            }
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }

                        Boolean isTrue = true;
                        Intent i = getIntent();
                        double longitude = i.getDoubleExtra("longitude",0);
                        for(Cluster m: mClusterManager.getAlgorithm().getItems()){
                            if(m.getPosition().longitude == longitude) {
                                isTrue = false;
                                // mRenderer.clusterMarkerMap.get(m).showInfoWindow();
                                moveCamera(m.getPosition(),DEFAULT_ZOOM);
                                break;
                            }
                        }
                        if (isTrue){
                            if (ActivityCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                    200,
                                    10, mLocationListener);
                        }
                    }
                });
            }

        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving camera to lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }


    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                mapFragment.getMapAsync(MapActivity.this);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(this,
                                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setZoomControlsEnabled(true);
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "map is ready!", Toast.LENGTH_SHORT).show();
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);

            geoLocate();

        }
    }

    // create menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // switch to a list view
            case R.id.list:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class UpdateTask extends AsyncTask<RestaurantManager, Integer, Integer> {
        AlertDialog UpdateDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
            LayoutInflater inflater = LayoutInflater.from(MapActivity.this);
            View view = inflater.inflate(R.layout.wait_dialog, null);
            UpdateDialog = builder.setView(view)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            UpdateDialog.dismiss();
                            updateTask.cancel(true);
                        }
                    })
                    .create();
            UpdateDialog.show();
        }

        @Override
        protected void onPostExecute(Integer add) {
            super.onPostExecute(sum);
            sum = add;
            UpdateDialog.dismiss();
            //geoLocate();
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(LAST_UPDATE, Calendar.getInstance().getTimeInMillis());
            editor.apply();
            Toast.makeText(MapActivity.this, "Sum after ASYNCTASK = " + sum, Toast.LENGTH_LONG).show();
        }

        @Override
        protected Integer doInBackground(RestaurantManager... restaurantManagers) {
            int add = 0;
            for(int i = 0; i < 1000; i++) {
                add++;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for(int i = 0; i < 1000; i++) {
                add++;
            }
            return add;
        }
    }
}
