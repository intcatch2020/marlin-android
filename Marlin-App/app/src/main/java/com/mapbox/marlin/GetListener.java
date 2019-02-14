package com.mapbox.marlin;

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

        try {
            //
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

            JSONObject boatPositionRead = response.getJSONObject("GPS");
            boatLatitude = boatPositionRead.getDouble("lat");
            boatLongitude = boatPositionRead.getDouble("lng");
            gpsSpeed = boatPositionRead.getDouble("speed");

            JSONObject boatAPS = response.getJSONObject("APS");
            boatRotation = boatAPS.getDouble("heading");
            calibration = boatAPS.getDouble("mag_cal");

            autoSpeed = response.getDouble("autonomy_speed");
            mode = response.getDouble("driving_mode"); // 0 = RC, 1 = Autonomy, 2 = Go Home

        } catch (JSONException e) {
            e.printStackTrace();
        }

        infoValueMap.put("Lat", boatLatitude);
        infoValueMap.put("Lng", boatLongitude);
        infoValueMap.put("Rot", boatRotation);
        infoValueMap.put("GpsSpd", gpsSpeed);
        infoValueMap.put("Calibration", calibration);
        infoValueMap.put("AutoSpd", autoSpeed);
        infoValueMap.put("Mode", mode);
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
    }
}
