package com.mapbox.marlin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Point;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.Style;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapLongClickListener {


    private DrawerLayout drawerLayout;
    private PermissionsManager permissionsManager;

    private MapboxMap map;
    private MapView mapView;
    private Style mapStyle;

    private Point phonePosition;
    private Point boatPosition;
    private double boatRotation;

    // View Object
    private FloatingActionButton cleanButton;
    private FloatingActionButton playButton;
    private FloatingActionButton centerBoatButton;
    private TextView txtView_1;
    private TextView txtView_2;
    private TextView txtView_3;
    private TextView txtView_IP;

    // My var
    private ArrayList<Marker> markerArrayList;
    private ArrayList<Polyline> lineArrayList;

    private Marker boatMarker;

    private boolean lineDrawed = false;
    
    //private String server_ip = "157.27.199.162"; //server pc
    private String server_ip = "192.168.2.1"; //server boat

    private Dialog_Connect dialog_connect;
    private Dialog_Speed dialog_speed;

    private TreeMap<String, Double> sensorsValueMap;
    private TreeMap<String, Double> boatPositionMap;


    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        dialog_connect = new Dialog_Connect();
        dialog_speed = new Dialog_Speed();

        mapView = findViewById(R.id.mapView);
        cleanButton = findViewById(R.id.cleanButton);
        playButton = findViewById(R.id.playButton);
        centerBoatButton = findViewById(R.id.centerBoatButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        txtView_1 = findViewById(R.id.textView_1);
        txtView_2 = findViewById(R.id.textView_2);
        txtView_3 = findViewById(R.id.textView_3);
        txtView_IP = findViewById(R.id.tv_IPaddress);
        NavigationView navigationView = findViewById(R.id.nav_view);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        final RequestQueue queue = Volley.newRequestQueue(this);

        // TEST STUFF
        sensorsValueMap = new TreeMap<>();
        sensorsValueMap.put("PH", -1.0);
        sensorsValueMap.put("DO", -1.0);
        sensorsValueMap.put("EC", -1.0);

        boatPositionMap = new TreeMap<>();
        boatPositionMap.put("Ltd", -1.0);
        boatPositionMap.put("Lng", -1.0);
        boatPositionMap.put("Rot", -1.0);
        // TEST STUFF

        markerArrayList = new ArrayList<>();
        lineArrayList = new ArrayList<>();
        boatPosition = Point.fromLngLat(11.02, 45.35);
        boatRotation = 0;

        navigationView.setNavigationItemSelectedListener( new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        //menuItem.setChecked(true);
                        //Toast.makeText(getApplicationContext(), "" + menuItem.getTitle(), Toast.LENGTH_LONG).show();

                        // close drawer when item is tapped
                        drawerLayout.closeDrawers();

                        if(menuItem.getItemId() == R.id.connect_to_boat)
                            dialog_connect.show(getSupportFragmentManager(), "Dialog_Connect");
                        else if (menuItem.getItemId() == R.id.set_speed)
                            dialog_speed.show(getSupportFragmentManager(), "Dialog_Speed");

                        return true;
                    }
                });


        final JsonObjectRequest jsonObjectRequestGet = new JsonObjectRequest(Request.Method.GET, "http://" + server_ip + ":5000/state", null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        double readDO = 0;
                        double readPH = 0;
                        double readEC = 0;

                        double boatLatitude = 0;
                        double boatLongitude = 0;
                        double speed = 0;

                        try {
                            JSONObject sensors = response.getJSONObject("sensors");
                            readDO = sensors.getDouble("do");
                            readEC = sensors.getDouble("ec");
                            readPH = sensors.getDouble("ph");

                            JSONObject boatPositionRead = response.getJSONObject("GPS");
                            boatLatitude = boatPositionRead.getDouble("lat");
                            boatLongitude = boatPositionRead.getDouble("lng");
                            speed = boatPositionRead.getDouble("speed");

                            JSONObject boatAPS = response.getJSONObject("APS");
                            boatRotation = boatAPS.getDouble("heading");

                        } catch (JSONException e) {
                            // Handle error
                        }

                        txtView_1.setText("PH\n" + String.format("%.2f", readPH));
                        txtView_2.setText("DO\n" + String.format("%.2f", readDO));
                        txtView_3.setText("EC\n" + String.format("%.2f", readEC));

                        txtView_IP.setText("Speed: " + speed);

                        boatPosition = Point.fromLngLat(boatLongitude, boatLatitude);
                        updateBoatMarkerPosition();
                        Log.d("Debug-dataread", "Read new data, boat pos: " + boatPosition.coordinates());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        txtView_1.setText("PH\n" + "--");
                        txtView_2.setText("DO\n" + "--");
                        txtView_3.setText("EC\n"+ "--");

                        Log.d("Debug-dataread", "Error while rading");
                        Log.d("Debug-dataread", error.toString());
                    }
                });

        //final JsonObjectRequest jsonObjectRequestGet = new JsonObjectRequest(Request.Method.GET, "http://" + server_ip + ":5000/state", null, new GetListener(sensorsValueList, boatPositionList), new GetListener(sensorsValueList, boatPositionList));
        //final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, "http://" + server_ip + ":5000/stop_autonomy", null, null, null);

        final Handler myHandler = new Handler();
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                queue.add(jsonObjectRequestGet);
                myHandler.postDelayed(this, 500);
                //server_ip = dialog_connect.ip;
                //txtView_IP.setText(server_ip);

                //txtView_1.setText(String.format("PH\n%s", sensorsValueMap.get("PH")));
                //txtView_2.setText(String.format("DO\n%s", sensorsValueMap.get("DO")));
                //txtView_3.setText(String.format("EC\n%s", sensorsValueMap.get("EC")));
            }
        }, 500);

        centerBoatButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCameraPosition(boatPosition.latitude(), boatPosition.longitude(), 15);
            }
        });

        cleanButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v){
                //Toast.makeText(getApplicationContext(), "Clean Map", Toast.LENGTH_LONG).show();

                for (Marker m : markerArrayList) {
                    map.removeMarker(m);
                }

                for (Polyline l : lineArrayList) {
                    map.removePolyline(l);
                }

                markerArrayList.clear();
                lineArrayList.clear();
                //cleanButton.isClickable(true);

                playButton.setEnabled(true);
                playButton.setClickable(true);
                playButton.setAlpha(1.0f);
                playButton.setImageResource(R.drawable.ic_wrong_directions);
                lineDrawed = false;

                final JsonObjectRequest stopAutonomyRequest= new JsonObjectRequest(Request.Method.POST, "http://" + server_ip + ":5000/stop_autonomy", null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Debug-datasend", "Sended");
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Debug-datasend", error.toString());
                    }
                });
                queue.add(stopAutonomyRequest);
            }
        });

        playButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v){
                //Toast.makeText(getApplicationContext(), "Write Lines", Toast.LENGTH_LONG).show();
                if(lineDrawed) {
                    Toast.makeText(getApplicationContext(), "Sending path...", Toast.LENGTH_LONG).show();

                    JSONObject pathToSend = null;
                    try {
                        pathToSend = createPathJSON();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d("Debug-jsongeneration", "JSON: " + pathToSend.toString());

                    final JsonObjectRequest jsonObjectRequestPost = new JsonObjectRequest(Request.Method.POST, "http://" + server_ip + ":5000/start_autonomy", pathToSend, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("Debug-datasend", "Sended");
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("Debug-datasend", error.toString());
                        }
                    });

                    queue.add(jsonObjectRequestPost);

                    lineDrawed = false;
                    playButton.setEnabled(false);
                    playButton.setClickable(false);
                    playButton.setAlpha(0.3f);
                }
                else {
                    for (int i=0; i < markerArrayList.size()-1; i++){
                        Polyline newLine = map.addPolyline(new PolylineOptions()
                                .add(markerArrayList.get(i).getPosition())
                                .add(markerArrayList.get(i+1).getPosition())
                                .width(3)
                        );
                        lineArrayList.add(newLine);
                    }
                    lineDrawed = true;
                    playButton.setImageResource(R.drawable.ic_play);

                }
            }
        });
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.addOnMapLongClickListener(this);

        mapboxMap.setStyle(new Style.Builder().fromUrl("mapbox://styles/mapbox/streets-v10"),
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        mapStyle = style;
                        enableLocationComponent(mapStyle);
                    }
        });

        map.getUiSettings().setRotateGesturesEnabled(false);
    }


    @Override
    public boolean onMapLongClick(@NonNull LatLng point) {
        //Toast.makeText(getApplicationContext(), "Added marker #" + markerArrayList.size(), Toast.LENGTH_LONG).show();

        Marker newMarker = map.addMarker(new MarkerOptions()
                                    .position(point)
                                    .title("Marker #" + markerArrayList.size())
        );
        markerArrayList.add(newMarker);
        Log.d("Debug-pos", "Marker Added " + markerArrayList.size());
        Log.d("Debug-pos", "@ pos: " + newMarker.getPosition());

        return true;
    }

    private void updateBoatMarkerPosition(){
        LatLng boatMarkerPos = new LatLng(boatPosition.latitude(), boatPosition.longitude());

        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);

        Bitmap bInput = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_white);
        Matrix matrix = new Matrix();
        matrix.setRotate((float)boatRotation);
        Bitmap bOutput = Bitmap.createBitmap(bInput, 0, 0, bInput.getWidth(), bInput.getHeight(), matrix, true);
        Icon icon = iconFactory.fromBitmap(bOutput);

        Log.d("Debug-putmarker", "boatmarker: " + boatRotation);

        if(map != null) {
            if(map.getMarkers().contains(boatMarker))
                map.removeMarker(boatMarker);

            boatMarker = map.addMarker(new MarkerOptions()
                    .position(boatMarkerPos)
                    .title("Boat Marker")
                    .icon(icon)
            );
        }
    }


    private void setCameraPosition(double latitude, double longitude, double zoom){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoom));
    }

    private JSONObject createPathJSON() throws JSONException {
        JSONObject mainObject = new JSONObject();

        JSONArray listObject = new JSONArray();
        for (Marker m : markerArrayList){
            JSONObject coordinate = new JSONObject();

            coordinate.put("lat", m.getPosition().getLatitude());
            coordinate.put("lng", m.getPosition().getLongitude());

            listObject.put(coordinate);
        }

        mainObject.put("path", listObject);

        return mainObject;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle){
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Get an instance of the component
            LocationComponent locationComponent = map.getLocationComponent();

            // Activate
            locationComponent.activateLocationComponent(this, loadedMapStyle);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        // Message for denied permission
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            map.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

}