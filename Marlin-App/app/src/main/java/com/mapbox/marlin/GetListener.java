package com.mapbox.marlin;

import android.os.Debug;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.TreeMap;

public class GetListener implements Response.Listener<JSONObject>, Response.ErrorListener {

    private TreeMap<String, SensorData> sensorsValueMap;
    private TreeMap<String, Double> infoValueMap;

    GetListener(TreeMap<String, SensorData> sensorsValueMap, TreeMap<String, Double> infoValueMap){
        this.sensorsValueMap = sensorsValueMap;
        this.infoValueMap = infoValueMap;
    }

    @Override
    public void onResponse(JSONObject response) {
        double boatLatitude = -1, boatLongitude = -1, gpsSpeed = -1;
        double boatRotation = -1, calibration = 0;
        double autoSpeed = 50, mode = 0;
        double pump_on= -1, pump_speed = -1, pump_time = -1;

        try {
            JSONArray sensors = response.getJSONArray("sensors");
            for (int i=0; i<sensors.length(); i++) {
                JSONObject obj = sensors.getJSONObject(i);
                String name = obj.getString("name");
                String unit = obj.getString("unit");
                double value = obj.getDouble("value");

                SensorData readed = new SensorData();
                readed.name = name;
                readed.unit = unit;
                readed.value = value;

                sensorsValueMap.put(name, readed);
            }
        } catch (JSONException e) { e.printStackTrace(); }

        try {
            JSONObject boatPositionRead = response.getJSONObject("GPS");
            boatLatitude = boatPositionRead.getDouble("lat");
            boatLongitude = boatPositionRead.getDouble("lng");
            gpsSpeed = boatPositionRead.getDouble("speed");
        } catch (JSONException e) { e.printStackTrace(); }

        try {
            JSONObject boatAPS = response.getJSONObject("APS");
            boatRotation = boatAPS.getDouble("heading");
            calibration = boatAPS.getDouble("mag_cal");
        } catch (JSONException e) { e.printStackTrace(); }

        try {
            JSONObject boatPump = response.getJSONObject("pump");
            boolean pump_on_flag = boatPump.getBoolean("active");
            if(pump_on_flag) pump_on = 1.;
            else  pump_on = 0.;
            pump_speed = boatPump.getDouble("speed");
            pump_time = boatPump.getDouble("time");
        } catch (JSONException e) { e.printStackTrace(); }

        try {
            autoSpeed = response.getDouble("autonomy_speed");
            mode = response.getDouble("driving_mode"); // 0 = RC, 1 = Autonomy, 2 = Go Home
        } catch (JSONException e) { e.printStackTrace(); }

        infoValueMap.put("Lat", boatLatitude);
        infoValueMap.put("Lng", boatLongitude);
        infoValueMap.put("Rot", boatRotation);
        infoValueMap.put("GpsSpd", gpsSpeed);
        infoValueMap.put("Calibration", calibration);
        infoValueMap.put("AutoSpd", autoSpeed);
        infoValueMap.put("Mode", mode);
        infoValueMap.put("Pump_on", pump_on);
        infoValueMap.put("Pump_speed", pump_speed);
        infoValueMap.put("Pump_time", pump_time);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
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
    }
}
