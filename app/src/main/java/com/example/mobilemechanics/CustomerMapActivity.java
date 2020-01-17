package com.example.mobilemechanics;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    LocationRequest locationRequest;
    Location lastLocation;


    private GoogleMap mMap;

    private LatLng pickUplLocation;
    private  Button mMechanics;
    private Button call;

    private TextView mMechanicName;
    private TextView mMechanicPhone;
    private TextView mMechanicCollege;

   // private RatingBar mRatingBar;

    private ImageView mMechanicImage;

    private LinearLayout mMechanicInfo;


    private FusedLocationProviderClient fusedLocationProviderClient;

    private Boolean requestBol = false;
    private Marker pickUpMarker;


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mMechanics = (Button)findViewById(R.id.mechanics);
        call = (Button) findViewById(R.id.call);

       // mRatingBar = (RatingBar)findViewById(R.id.ratings);

        //related to displaying the information of mechanic in customerMaps
        mMechanicPhone = (TextView)findViewById(R.id.mechanicPhone);
        mMechanicName =(TextView)findViewById(R.id.mechanicName);
        mMechanicCollege=(TextView)findViewById(R.id.mechanicCollege);

        mMechanicImage = (ImageView)findViewById(R.id.mechanicProfileImage);

        mMechanicInfo = (LinearLayout)findViewById(R.id.mechanicInfo);


        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null){
                    Intent intent = new Intent(CustomerMapActivity.this, AvailableMechanicsActivity.class);
                    startActivity(intent);
                    finish();return;

                }
            }
        };


        mMechanics.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, AvailableMechanicsActivity.class);
                startActivity(intent);
                return;
            }
        });

        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol){
                    requestBol=false;
                    geoQuery.removeAllListeners();
                    mechanicLocationRef.removeEventListener(mechanicLocationRefListener);

                    if(mechanicFoundId !=null){
                        DatabaseReference mechanicsref = FirebaseDatabase.getInstance().getReference().child("Users").child("mechanics").child(mechanicFoundId);
                        mechanicsref.setValue(true);
                        mechanicFoundId=null;

                    }
                    mechanicFound=false;
                    radius=3;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequests");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);

                    if(pickUpMarker !=null){
                        pickUpMarker.remove();
                    }
                    call.setText("call mechanic");

                    mMechanicInfo.setVisibility(View.GONE);
                    mMechanicName.setText("");
                    mMechanicPhone.setText("");
                    mMechanicImage.setImageResource(R.mipmap.ic_launcher);
                }
                else{
                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequests");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                    pickUplLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(pickUplLocation).title("come here"));


                    call.setText("getting you a mechanic");

                    getClossestMechanic();
                    }
                }


        });
    }

    private  int radius = 3;
    private Boolean mechanicFound = false;
    private String mechanicFoundId ;

    GeoQuery geoQuery;
    private void getClossestMechanic(){
        DatabaseReference mechaniclocation = FirebaseDatabase.getInstance().getReference().child("mechanicsAvailable");
        GeoFire geoFire = new GeoFire(mechaniclocation);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUplLocation.latitude,pickUplLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // assumption is the first driver within the fast radius will be called even if there are many within one one place
                //first to be found, the first to be chosen

                //String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                if (!mechanicFound && requestBol){
                    mechanicFound = true;
                    mechanicFoundId = key;

                    DatabaseReference mechanicsref = FirebaseDatabase.getInstance().getReference().child("Users").child("mechanics").child( mechanicFoundId).child("CustomerRequests");
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    HashMap map = new HashMap();
                    map.put("CustomerServiceId  ", customerId);
                    mechanicsref.updateChildren(map);

                    getMechanicLocation();
                    getMechanicInfomation();
                    call.setText("looking for the mechanic location");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!mechanicFound)
                {
                    radius++;
                    getClossestMechanic();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

        }

    private void getMechanicInfomation() {
        mMechanicInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("mechanics").child(mechanicFoundId);

        mCustomerDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if(map.get("mechanicName") !=null){
                        mMechanicName.setText(map.get("mechanicName").toString());
                    }
                    if(map.get("mechanicPhone") !=null){
                        mMechanicPhone.setText(map.get("mechanicPhone").toString());
                    }
                    if(map.get("mechanicCollege") !=null){
                        mMechanicCollege.setText(map.get("mechanicCollege").toString());
                    }
                    if (map.get("profileImageUrl")!=null){
                        Glide.with(getApplicationContext()).load(map.get("profileImageUrl").toString()).into(mMechanicImage);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private Marker mechanicMarker;
    private ValueEventListener mechanicLocationRefListener;
    private  DatabaseReference mechanicLocationRef;


    private void getMechanicLocation(){
        mechanicLocationRef = FirebaseDatabase.getInstance().getReference().child("mechanicsWorking").child(mechanicFoundId).child("l");
        mechanicLocationRefListener = mechanicLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    call.setText("mechanic has been found");

                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng mechanicLatLng = new LatLng(locationLat,locationLng);

                    if (mechanicMarker !=null){
                        mechanicMarker.remove();
                    }
                    mechanicMarker = mMap.addMarker(new MarkerOptions().position(mechanicLatLng).title("your mechanic"));


                    Location loc1 = new Location("");
                    loc1.setLatitude(pickUplLocation.latitude);
                    loc1.setLongitude(pickUplLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(mechanicLatLng.latitude);
                    loc2.setLongitude(mechanicLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance<100){

                        call.setText("mechanic is here");
                    }else {

                        call.setText("mechanic has been found" + String.valueOf(distance));
                    }

                 }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                locationRequest = new LocationRequest();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(1000);
                locationRequest.setFastestInterval(1000);
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, mlocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);


            } else {
                checkLocationPermission();
            }
        }


    }

    LocationCallback mlocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location: locationResult.getLocations()) {
                if (getApplicationContext() !=null) {

                    lastLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

                    if(!getMechanicsAroundStarted)
                        getMechanicsAvailable();

                }
            }
        }
    };

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                    ActivityCompat.requestPermissions(CustomerMapActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                    .create()
                    .show();
            }
            else {
                ActivityCompat.requestPermissions(CustomerMapActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, mlocationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);

                }
            } else {
                Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(mlocationCallback);
        }

    }

    boolean getMechanicsAroundStarted = false;
    List<Marker> markersList = new ArrayList<Marker>();

    private void getMechanicsAvailable () {

        getMechanicsAroundStarted = true;
        DatabaseReference mechanicLocation = FirebaseDatabase.getInstance().getReference().child("mechanicsAvailable");

        GeoFire geoFire = new GeoFire(mechanicLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(lastLocation.getLongitude(), lastLocation.getLatitude()), 1000);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //this ensures that we dont have two markers that are the same
                for (Marker markerIt : markersList) {
                    if (markerIt.getTag().equals(key))
                        return;
                }
                LatLng mechanicLocation = new LatLng(location.latitude, location.longitude);
                Marker mMechanicMarker = mMap.addMarker(new MarkerOptions().position(mechanicLocation).title(key).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)));
                mMechanicMarker.setTag(key);

                markersList.add(mMechanicMarker);


            }

            @Override
            public void onKeyExited(String key) {
                for (Marker markerIt : markersList) {
                    if (markerIt.getTag().equals(key)) {
                        markerIt.remove();
                    }
                }
            }


            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for (Marker markerIt : markersList) {
                    if (markerIt.getTag().equals(key)) {
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                    }
                }
            }


            @Override
            public void onGeoQueryReady() {


            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

}