package com.example.flex;

import static android.app.PendingIntent.getActivity;
import static android.view.animation.AnimationUtils.loadAnimation;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CHECK_SETTINGS = 1001;
    public static GoogleMap mMap;
    private FloatingActionButton fab,fab1,fab2;
    private Handler handler;
    private long refreshTime = 5000;
    Runnable runnable;


    private FirebaseAuth mAuth;
    private FirebaseDatabase database=FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference,LongitudeRef,LatitudeRef;


    private final int FINE_LOCATION_PERMISSION_REQUEST_CODE = 1;
    private final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 3;

    FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    Location currentLocation, Database_Location;

    private Animation fabOpen, fabClose;
    private boolean isFabOpen = false;

    private RecyclerView userRecyclerView;
    public static UserAdapter userAdapter;
    public static List<User> userList = new ArrayList<>();
    private Map<String, Marker> markerMap = new HashMap<>();

    Double lon = 0.0;
    Double lat = 0.0;
//    String userID = mAuth.getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
//        getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, new profile()).commit();
        Fragment selectedFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                if (item.getItemId() == R.id.nav_home) {
//                    findViewById(R.id.frame_layout).setVisibility(View.GONE);
//                    findViewById(R.id.fragment).setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, new homeFragment()).commit();
                } else if (item.getItemId() == R.id.nav_user) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, new UserFragment()).commit();
                } else if (item.getItemId() == R.id.nav_profile) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, new profile()).commit();
//                    findViewById(R.id.frame_layout).setVisibility(View.VISIBLE);
//                    findViewById(R.id.fragment).setVisibility(View.GONE);
                }
                return true;
            }
        });

        //toolbar Section
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("IntroxMap");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationIcon(R.drawable.baseline_density_medium_24);
        toolbar.setNavigationOnClickListener(v ->
                toggleList());
        toolbar.setNavigationContentDescription("Toggle List");

        mAuth = FirebaseAuth.getInstance();

        checkLocationSettings();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//        getCurrentLocation();
        setupLocationCallback();
        Floating();
        checkLocationPermission();
        checkNotificationPermission();

        userRecyclerView = findViewById(R.id.userRecyclerView);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(userList, user -> {
            Marker marker = markerMap.get(user.getName());
            if (mMap != null) {
                mMap.clear();

                LatLng location = new LatLng(user.getLatitude(), user.getLongitude());
                mMap.addMarker(new MarkerOptions().position(location).title(user.getName()));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.5f));
            }
        });

        userRecyclerView.setAdapter(userAdapter);

    }

//    private void userStatus() {
//        String userID = mAuth.getCurrentUser().getUid();
//        DatabaseReference userStatusReference = FirebaseDatabase.getInstance().getReference("user").child(userID).child("online");
//        userStatusReference.setValue(true);
//        userStatusReference.onDisconnect().setValue(false);
//    }

    private void userStatus() {
        String userID = mAuth.getCurrentUser().getUid();
        DatabaseReference userStatusReference = FirebaseDatabase.getInstance().getReference("user").child(userID).child("online");
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(connected)) {
                    userStatusReference.setValue(true);
                    userStatusReference.onDisconnect().setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("userStatus", "onDisconnect listener failed: " + error.getMessage());
            }
        });
    }

    private void loadUsers() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> newList = new ArrayList<>();
                markerMap.clear();
                mMap.clear();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String name = userSnapshot.child("name").getValue(String.class);
                    Double lat = userSnapshot.child("latitude").getValue(Double.class);
                    Double lng = userSnapshot.child("longitude").getValue(Double.class);
                    boolean online = Boolean.TRUE.equals(userSnapshot.child("online").getValue(Boolean.class));
                    String userID = userSnapshot.getKey();

                    if (name != null || lat != null || lng != null) {

//                    LatLng location = new LatLng(lat, lng);
//                    Marker marker = mMap.addMarker(new MarkerOptions().position(location).title(name));
//
//                    if (marker != null) {
//                        markerMap.put(name, marker);
//                        fabOpen = loadAnimation(MainActivity.this, R.anim.fab_open);
//                    }

                    newList.add(new User(name, lat, lng, online));
                    Log.d("FirebaseData", "User: " + name);  // 🟢 Debugging ke liye
                        }
                }

                userAdapter.updateList(newList);  // 🟢 List ko update karo
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            // GPS is ON, start location updates
            startLocationUpdates();
        });

        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    // Ask user to enable GPS
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException ignored) {
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // User enabled GPS, start location updates
                startLocationUpdates();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getCurrentLocation();
                    }
                }, 5000); // ⏱ 5000 milliseconds = 5 seconds ka delay

            }
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, FINE_LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE);
                Toast.makeText(MainActivity.this, "Location permission denied. Please allow! All Time Location", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
//                    updateMap(currentLocation);
                    Log.i("LocationUpdate", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
                }
            }
        };
    }

    public void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
                    mapFragment.getMapAsync(MainActivity.this);

                    String userID = mAuth.getCurrentUser().getUid();
                    databaseReference = FirebaseDatabase.getInstance().getReference("user").child(userID);
                    databaseReference.child("latitude").setValue(location.getLatitude());
                    databaseReference.child("longitude").setValue(location.getLongitude());

                    Log.i("XOXO", "Location: " + location.getLatitude() + " " + location.getLongitude());
                }
            }
        });
    }

//    private void getCurrentLocation() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
//                if (location != null) {
//                    currentLocation = location;
//                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
//                    mapFragment.getMapAsync(MainActivity.this);
//                }
//            });
//        } else {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//        }
//    }

    public void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // Update every 10 seconds
        locationRequest.setFastestInterval(2000); // Fastest update interval
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Use highest accuracy

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateMap(Location location) {
        if (mMap != null) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.addMarker((new MarkerOptions()).position(currentLatLng).title("Current Location"));
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15.5f));
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        loadUsers();
        userStatus();

        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        LatLng sydney = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Current Location"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15.5f));

        Log.d("Intros","Introx_Location   " + "Longitude: " +currentLocation.getLongitude()+ "   " + "Latitude: " + currentLocation.getLatitude());
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationUpdates();
        getCurrentLocation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
//        getCurrentLocation();
        userStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }


    private MenuItem refreshItem;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        refreshItem = menu.findItem(R.id.refresh);
        return true;
    }

    //    Menu function set
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.startSharingLocation) {
            requestForegroundServicePermission();
        }
        if (id == R.id.stopSharingLocation) {
            stopLocationSharing();
        }
        if (id == R.id.satelliteMenu) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            return true;
        } else if (id == R.id.refresh) {
            startRefreshAnimation();
            getCurrentLocation();
        } else if (id == R.id.adminLogin) {
            adminLogin();
        } else if (id==R.id.signOut) {
            userSignOut();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isListVisible = true;
    private void toggleList() {
        if (isListVisible) {
            userRecyclerView.setVisibility(View.GONE);
            isListVisible = false;
        } else {
            userRecyclerView.setVisibility(View.VISIBLE);
            isListVisible = true;
        }
    }

    private void startRefreshAnimation() {
        if (refreshItem != null) {
            // Set Custom Animation on Menu Icon
            ImageView refreshIcon = new ImageView(this);
            refreshIcon.setImageDrawable(refreshItem.getIcon());

            Animation rotateAnim = loadAnimation(this, R.anim.rotate_refresh);
            refreshIcon.startAnimation(rotateAnim);

            refreshItem.setActionView(refreshIcon);

            // Reset Icon after Animation
            refreshIcon.postDelayed(() -> refreshItem.setActionView(null), 1100);
        }
    }

    private static final int FOREGROUND_SERVICE_PERMISSION_CODE = 101;

    private void requestForegroundServicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34 (Android 14)
            if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.FOREGROUND_SERVICE_LOCATION}, FOREGROUND_SERVICE_PERMISSION_CODE);
            } else {
                startLocationSharing(); // Agar permission already mili hui hai
            }
        } else {
            startLocationSharing(); // Android 14 se niche ke versions me permission runtime par nahi chahiye
        }
    }


    public void startLocationSharing() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(MainActivity.this, LocationWorker.class);
//            startService(intent);
//            Toast.makeText(MainActivity.this, "Location sharing started", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(MainActivity.this, "Location sharing not started", Toast.LENGTH_SHORT).show();
//            showSettingsDialog();
//        }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent); // ✅ This is key
            } else {
                startService(intent);
            }

            Toast.makeText(MainActivity.this, "Location sharing started", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Location sharing not started", Toast.LENGTH_SHORT).show();
            showSettingsDialog(); // Ask user to enable permission
        }
    }

    public void stopLocationSharing() {
        Intent stopIntent = new Intent("STOP_SERVICE");
        sendBroadcast(stopIntent);
        Toast.makeText(this, "Location sharing stopped", Toast.LENGTH_SHORT).show();
    }

    public void userSignOut() {
        FirebaseAuth.getInstance().signOut();
        stopLocationSharing();
        Intent intent = new Intent(MainActivity.this, login.class);
        startActivity(intent);
        Toast.makeText(MainActivity.this, "SigOut Successful..", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void Floating() {
        FloatingActionButton fab_terrain = findViewById(R.id.terrain);
        FloatingActionButton fab_hybrid = findViewById(R.id.hybrid);
        fab = findViewById(R.id.mode);
        fab1 = findViewById(R.id.satelliteMode);
        fab2 = findViewById(R.id.NormalMode);
        fabOpen = loadAnimation(this, R.anim.fab_open);
        fabClose = loadAnimation(this, R.anim.fab_close);

        // Visibility Set Invisible
        fab1.setVisibility(View.INVISIBLE);
        fab2.setVisibility(View.INVISIBLE);
        fab_terrain.setVisibility(View.INVISIBLE);
        fab_hybrid.setVisibility(View.INVISIBLE);

        // floating button click visibility event
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fab1.getVisibility() == View.INVISIBLE) {
                    fab1.setVisibility(View.VISIBLE);
                    fab2.setVisibility(View.VISIBLE);
                    fab_terrain.setVisibility(View.VISIBLE);
                    fab_hybrid.setVisibility(View.VISIBLE);
                    fab1.startAnimation(fabOpen);
                    fab2.startAnimation(fabOpen);
                    fab_terrain.startAnimation(fabOpen);
                    fab_hybrid.startAnimation(fabOpen);
                } else {
                    fab1.setVisibility(View.INVISIBLE);
                    fab2.setVisibility(View.INVISIBLE);
                    fab_terrain.setVisibility(View.INVISIBLE);
                    fab_hybrid.setVisibility(View.INVISIBLE);
                    fab1.startAnimation(fabClose);
                    fab2.startAnimation(fabClose);
                    fab_terrain.startAnimation(fabClose);
                    fab_hybrid.startAnimation(fabClose);
                }
            }
        });

        // floating button click event for satellite
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            }
        });
        // floating button click event for normal
//        fab2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//            }
//        });
        fab2.setOnClickListener(v -> {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        });
        // floating button click event for terrain
        fab_terrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            }
        });
        // floating button click event for hybrid
        fab_hybrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            }
        });
    }

    //    check device location permission allowed or not.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_LOCATION_PERMISSION_REQUEST_CODE || requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

                if (requestCode == FOREGROUND_SERVICE_PERMISSION_CODE) {
                    startLocationSharing();
                } else {
                    Toast.makeText(MainActivity.this, "Foreground Service Permission Denied", Toast.LENGTH_SHORT).show();
                }
                if (!showRationale) {
                    // 🚨  "Don't Ask Again" select kiya hai
                    showSettingsDialog();
                } else {
                    Toast.makeText(MainActivity.this, "Location permission denied. Please allow!", Toast.LENGTH_SHORT).show();
                    checkLocationPermission();
                }
            }
        }
    }
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Need Location Permission");
        builder.setMessage("This app needs location permission to work. Enable it in Settings. Please Allow All time Location");

        builder.setPositiveButton("Go to Settings", (dialog, which) -> {
            dialog.dismiss();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    public void adminLogin() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user");
        Toast.makeText(MainActivity.this, "Admin login Successful", Toast.LENGTH_SHORT).show();

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mMap.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String name = userSnapshot.child("name").getValue(String.class);
                    double lat = userSnapshot.child("latitude").getValue(Double.class);
                    double lng = userSnapshot.child("longitude").getValue(Double.class);

                    LatLng location = new LatLng(lat, lng);
                    mMap.addMarker(new MarkerOptions().position(location).title(name)).showInfoWindow();
                    Toast.makeText(MainActivity.this, name +" Location Refresh", Toast.LENGTH_SHORT).show();
                    Log.d("ADAD", "Name: " + name + ", Latitude: " + lat + ", Longitude: " + lng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching data", error.toException());
            }
        });

    }

}