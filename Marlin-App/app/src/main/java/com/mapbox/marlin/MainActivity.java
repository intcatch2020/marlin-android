package com.mapbox.marlin;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Debug;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;

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

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapLongClickListener {

    private DrawerLayout drawerLayout;
    private PermissionsManager permissionsManager;
    private MapboxMap map;
    private MapView mapView;
    private Style mapStyle;

    // View Object
    private FloatingActionButton clearButton;
    private FloatingActionButton playButton;
    private FloatingActionButton centerBoatButton;
    private FloatingActionButton spiralPathButton;
    private FloatingActionButton standardPathButton;
    private TextView txtView_miniLog;
    private TextView txtView_pumpLog;
    private NavigationView navigationView;

    private LinearLayout sensorTxtContainer;
    private ArrayList<TextView> sensorsTextViewList;

    // My var
    private ArrayList<Marker> markerArrayListGraphic;
    private ArrayList<Marker> markerArrayListLogic;
    private ArrayList<Polyline> lineArrayList;

    private Marker boatMarker;
    private Marker selectedMarker;

    private JsonObjectRequest jsonObjectRequestGet;

    //private String server_ip = "157.27.198.83"; //server pc
    //private String server_ip = "192.168.2.1"; //server boat
    //private String server_ip = "157.27.193.198"; //server my
    private String server_ip = "xxx.xxx.xxx.xxx";

    private Dialog_Connect dialog_connect;
    private Dialog_Speed dialog_speed;
    private Dialog_Peristaltic dialog_peristaltic;

    private TreeMap<String, SensorData> sensorsValueMap;
    private TreeMap<String, Double> infoValueMap;


    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        // Class constructor
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        // Get all view elements
        mapView = findViewById(R.id.mapView);
        clearButton = findViewById(R.id.clearButton);
        playButton = findViewById(R.id.playButton);
        centerBoatButton = findViewById(R.id.centerBoatButton);
        spiralPathButton = findViewById(R.id.spiralPathButton);
        standardPathButton = findViewById(R.id.standardPathButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        txtView_miniLog = findViewById(R.id.textView_miniLog);
        txtView_pumpLog = findViewById(R.id.textView_pumpLog);
        navigationView = findViewById(R.id.nav_view);

        // Get all the sensors view
        sensorsTextViewList = new ArrayList<>();
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_1));
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_2));
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_3));
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_4));
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_5));
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_6));
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_7));
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_8));
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_9));
        sensorsTextViewList.add((TextView) findViewById(R.id.textView_10));

        // Initialize MapBox
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Toolbar and actionbar stuff
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        // Initialize server request queue
        final RequestQueue queue = Volley.newRequestQueue(this);

        // Initialize sensors map
        sensorsValueMap = new TreeMap<>();
        infoValueMap = new TreeMap<>();
        infoValueMap.put("Lat", -1.);
        infoValueMap.put("Lng", -1.);
        infoValueMap.put("Rot", -1.);
        infoValueMap.put("GpsSpd", -1.);
        infoValueMap.put("Calibration", -1.);
        infoValueMap.put("AutoSpd", -1.);
        infoValueMap.put("Mode", -1.);
        infoValueMap.put("Pump_on", -1.);
        infoValueMap.put("Pump_speed", -1.);
        infoValueMap.put("Pump_time", -1.);

        // Initialize other variables
        markerArrayListGraphic = new ArrayList<>();
        markerArrayListLogic = new ArrayList<>();
        lineArrayList = new ArrayList<>();
        selectedMarker = null;

        // Initialize periodic GET request
        jsonObjectRequestGet = new JsonObjectRequest(Request.Method.GET, "http://" + server_ip + ":5000/state", null, new GetListener(sensorsValueMap, infoValueMap), new GetListener(sensorsValueMap, infoValueMap));

        // Initialize dialog windows
        dialog_connect = new Dialog_Connect();
        dialog_speed = new Dialog_Speed();
        dialog_peristaltic = new Dialog_Peristaltic();

        // Set-up periodic cycle
        final Handler myHandler = new Handler();
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                queue.add(jsonObjectRequestGet);
                myHandler.postDelayed(this, 500);

                server_ip = Dialog_Connect.ip;
                jsonObjectRequestGet = new JsonObjectRequest(Request.Method.GET, "http://" + server_ip + ":5000/state", null, new GetListener(sensorsValueMap, infoValueMap), new GetListener(sensorsValueMap, infoValueMap));

                updateMiniLogValues();
                updatePumpLogValues();
                updateSensorView();

                updateBoatMarkerPosition();

                updateAutonomySpeedView();
                updatePeristalticView();
            }
        }, 500);

        // Navigation View menu listener
        navigationView.setNavigationItemSelectedListener( new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NotNull MenuItem menuItem) {
                        drawerLayout.closeDrawers();

                        if(menuItem.getItemId() == R.id.connect_to_boat) {
                            dialog_connect.show(getSupportFragmentManager(), "Dialog_Connect");
                        } else if (menuItem.getItemId() == R.id.set_speed) {
                            Dialog_Speed.server_ip = server_ip;
                            Dialog_Speed.queue = queue;
                            dialog_speed.show(getSupportFragmentManager(), "Dialog_Speed");
                        } else if (menuItem.getItemId() == R.id.peristaltic_pump) {
                            Dialog_Peristaltic.server_ip = server_ip;
                            Dialog_Peristaltic.queue = queue;
                            dialog_peristaltic.show(getSupportFragmentManager(), "Dialog_Peristaltic");
                        }

                        return true;
                    }

                });

        // Center Button Listener
        centerBoatButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(infoValueMap.get("Lat") == -1 && infoValueMap.get("Lat") == -1)
                    Toast.makeText(getApplicationContext(), "No GPS signal!", Toast.LENGTH_LONG).show();
                else
                    setCameraPosition(infoValueMap.get("Lat"), infoValueMap.get("Lng"));
            }
        });

        // Clear Button Listener
        clearButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v){
                // Clear marker and line graphic
                for (Marker m : markerArrayListGraphic)
                    map.removeMarker(m);
                for (Polyline l : lineArrayList)
                    map.removePolyline(l);

                // Clear marker and line logic
                markerArrayListGraphic.clear();
                markerArrayListLogic.clear();
                lineArrayList.clear();
                selectedMarker = null;

                // Re-Enable the path maker buttons
                enableButton(spiralPathButton);
                enableButton(standardPathButton);
                disableButton(playButton);

                // POST request for stop autonomy
                queue.add(new JsonObjectRequest(Request.Method.POST, "http://" + server_ip + ":5000/stop_autonomy", null, null, null));
            }
        });

        // Spiral button listener, create the spiral path (graphic for the line, logic for marker)
        spiralPathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if(markerArrayListGraphic.size() < 3)
                    Toast.makeText(getApplicationContext(), "I need at least 3 points for this!", Toast.LENGTH_LONG).show();
                else {
                    PathPlanner pathPlanner = new PathPlanner();
                    pathPlanner.setPoints(markerArrayListGraphic);
                    //pathPlanner.setPoints(markerArrayListLogic);
                    markerArrayListLogic = pathPlanner.getSpiralPath(3);

                    for (int i = 0; i < markerArrayListLogic.size() - 1; i++) {
                        Polyline newLine = map.addPolyline(new PolylineOptions()
                                .add(markerArrayListLogic.get(i).getPosition())
                                .add(markerArrayListLogic.get(i + 1).getPosition())
                                .width(3));
                        lineArrayList.add(newLine);
                    }

                    enableButton(playButton);
                    disableButton(spiralPathButton);
                    disableButton(standardPathButton);
                }
            }
        });

        // Standard button listener, create the standard path (graphic for the line, logic for marker)
        standardPathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if(markerArrayListGraphic.size() < 1) {
                    Toast.makeText(getApplicationContext(), "I need at least 1 point for this!", Toast.LENGTH_LONG).show();
                } else {
                    PathPlanner pathPlanner = new PathPlanner();
                    pathPlanner.setPoints(markerArrayListGraphic);
                    //pathPlanner.setPoints(markerArrayListLogic);
                    markerArrayListLogic = pathPlanner.getStandardPath();

                    for (int i = 0; i < markerArrayListLogic.size() - 1; i++) {
                        Polyline newLine = map.addPolyline(new PolylineOptions()
                                .add(markerArrayListLogic.get(i).getPosition())
                                .add(markerArrayListLogic.get(i + 1).getPosition())
                                .width(3));
                        lineArrayList.add(newLine);
                    }

                    enableButton(playButton);
                    disableButton(spiralPathButton);
                    disableButton(standardPathButton);
                }
            }
        });

        // Play Button Listener, send path (logic marker)
        playButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Toast.makeText(getApplicationContext(), "Sending path...", Toast.LENGTH_LONG).show();

                queue.add(new JsonObjectRequest(Request.Method.POST, "http://" + server_ip + ":5000/start_autonomy", createPathJSON(), null, null));

                disableButton(playButton);
            }
        });

    }

    @Override
    public void onMapReady(@NotNull MapboxMap mapboxMap) {
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

        map.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker)
            {
                boolean done = false;

                if(selectedMarker == null) {
                    selectedMarker = marker;

                    // Edit icon and add alfa when clicked
                    Bitmap bInput = selectedMarker.getIcon().getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(bInput);
                    int color = (100 & 0xFF) << 24;
                    canvas.drawColor(color, PorterDuff.Mode.DST_IN);
                    IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
                    Icon icon = iconFactory.fromBitmap(bInput);
                    selectedMarker.setIcon(icon);
                    done = false;
                } else {
                    Marker newMarker = map.addMarker(new MarkerOptions()
                            .position(selectedMarker.getPosition())
                            .title("Move marker #" + markerArrayListGraphic.size())
                    );
                    map.removeMarker(selectedMarker);
                    markerArrayListGraphic.set(markerArrayListGraphic.indexOf(selectedMarker), newMarker);
                    selectedMarker = null;
                    done = true;
                }

                return done;
            }
        });

        map.addOnMapClickListener(new MapboxMap.OnMapClickListener()
        {
            @Override
            public boolean onMapClick(@NonNull final LatLng point) {
                if(selectedMarker != null) {

                    Marker newMarker = map.addMarker(new MarkerOptions()
                            .position(point)
                            .title("Move marker #" + markerArrayListGraphic.size())
                    );
                    map.removeMarker(selectedMarker);

                    markerArrayListGraphic.set(markerArrayListGraphic.indexOf(selectedMarker), newMarker);

                    if(lineArrayList.size() > 0) {
                        PathPlanner pathPlanner = new PathPlanner();
                        pathPlanner.setPoints(markerArrayListGraphic);

                        if(markerArrayListLogic.size() == markerArrayListGraphic.size())
                            markerArrayListLogic = pathPlanner.getStandardPath();
                        else
                            markerArrayListLogic = pathPlanner.getSpiralPath(3);

                        for (Polyline l : lineArrayList)
                            map.removePolyline(l);

                        for (int i = 0; i < markerArrayListLogic.size() - 1; i++) {
                            Polyline newLine = map.addPolyline(new PolylineOptions()
                                    .add(markerArrayListLogic.get(i).getPosition())
                                    .add(markerArrayListLogic.get(i + 1).getPosition())
                                    .width(3));
                            lineArrayList.add(newLine);
                        }
                    }

                    selectedMarker = null;
                }

                return true;
            }
        });


    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng point) {
        Marker newMarker = map.addMarker(new MarkerOptions()
                                    .position(point)
                                    .title("Move marker #" + markerArrayListGraphic.size())
        );
        markerArrayListGraphic.add(newMarker);
        markerArrayListLogic.add(newMarker);
        return true;
    }

    private void updateBoatMarkerPosition(){
        double boatLat = infoValueMap.get("Lat");
        double boatLng = infoValueMap.get("Lng");
        double boatRot = infoValueMap.get("Rot");

        LatLng boatMarkerPos = new LatLng(boatLat, boatLng);

        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);

        Bitmap bInput = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_white);
        Matrix matrix = new Matrix();
        matrix.setRotate((float)boatRot);
        Bitmap bOutput = Bitmap.createBitmap(bInput, 0, 0, bInput.getWidth(), bInput.getHeight(), matrix, true);
        Icon icon = iconFactory.fromBitmap(bOutput);

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

    private void updateAutonomySpeedView(){
        double value = infoValueMap.get("AutoSpd");
        if(value != -1)
         Dialog_Speed.selectedSpeed = (int)value;
    }

    private void updatePeristalticView(){
        int pump_on = infoValueMap.get("Pump_on").intValue();

        if(pump_on == 1.) Dialog_Peristaltic.pump_active = true;
        else Dialog_Peristaltic.pump_active = false;
    }

    @SuppressLint("DefaultLocale")
    private void updateSensorView(){

        int i = 0;
        for (Map.Entry<String, SensorData> entry : sensorsValueMap.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue().value;
            String unit = entry.getValue().unit;
            sensorsTextViewList.get(i).setText(String.format("%s\n%.2f\n%s", key, value, unit));
            sensorsTextViewList.get(i).setVisibility(View.VISIBLE);
            i++;
        }

        for (int j=i; j < sensorsTextViewList.size(); j++){
            sensorsTextViewList.get(j).setVisibility(View.GONE);
        }
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateMiniLogValues(){

        String ip = server_ip;
        double speed = infoValueMap.get("GpsSpd");
        int calibration = infoValueMap.get("Calibration").intValue();
        int mode = infoValueMap.get("Mode").intValue();

        String modeStr;
        switch (mode) {
            case 0:
                modeStr = "RC";
                break;
            case 1:
                modeStr = "Autonomy";
                break;
            case 2:
                modeStr = "Go Home";
                break;
            default:
                modeStr = "Unknown";
                break;
        }

        String miniLog = "";
        miniLog += String.format("IP: %s \n", ip);
        miniLog += String.format("Speed: %s \n", speed);
        miniLog += String.format("Compass Calibration: %d/3 \n", calibration);
        miniLog += String.format("Driving Mode: %s", modeStr);

        if (calibration == -1){
            // Use calibration as a value, -1 the boat is disconnected, if {0, 1, 2} boat connected
            txtView_miniLog.setText("Failed to connect to: " + ip);
            txtView_miniLog.setTextColor(Color.rgb(255, 0, 0));
        }
        else {
            txtView_miniLog.setText(miniLog);
            txtView_miniLog.setTextColor(Color.rgb(0, 0, 0));
        }
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updatePumpLogValues(){

        int pump_on = infoValueMap.get("Pump_on").intValue();
        int pump_speed = infoValueMap.get("Pump_speed").intValue();
        double pump_time = infoValueMap.get("Pump_time");

        String pumpLog = "";
        pumpLog += String.format("Pump ON\n");
        pumpLog += String.format("Pump Speed: %d \n", pump_speed);
        pumpLog += String.format("Remaining time: %4.2f (s)", pump_time);

        txtView_pumpLog.setText(pumpLog);

        if(pump_on == 1.) {
            txtView_pumpLog.setVisibility(View.VISIBLE);
            txtView_pumpLog.setText(pumpLog);
            txtView_pumpLog.setTextColor(Color.rgb(0, 0, 0));
        } else {
            txtView_pumpLog.setVisibility(View.INVISIBLE);
        }

    }

    private JSONObject createPathJSON() {
        JSONObject mainObject = new JSONObject();

        try {
            JSONArray listObject = new JSONArray();
            for (Marker m : markerArrayListLogic){
                JSONObject coordinate = new JSONObject();

                coordinate.put("lat", m.getPosition().getLatitude());
                coordinate.put("lng", m.getPosition().getLongitude());

                listObject.put(coordinate);
            }
            mainObject.put("path", listObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return mainObject;
    }

    private void setCameraPosition(double latitude, double longitude){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), (double) 15));
    }

    private void disableButton(FloatingActionButton button){
        button.setEnabled(false);
        button.setClickable(false);
        button.setAlpha(0.3f);
    }

    private void enableButton(FloatingActionButton button){
        button.setEnabled(true);
        button.setClickable(true);
        button.setAlpha(1.f);
    }

    //////////////////////////////////////////////////////////////////////////
    // =========================== END OF CODE =========================== //
    /////////////////////////////////////////////////////////////////////////

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
            LocationComponent locationComponent = map.getLocationComponent();

            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
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