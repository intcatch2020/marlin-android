package com.mapbox.marlin;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.TreeMap;

public class GetListener implements Response.Listener<JSONObject>, Response.ErrorListener {

    private TreeMap<String, Double> sensorsValueMap;
    private TreeMap<String, Double> boatPositionMap;

    GetListener(TreeMap<String, Double> sensorsValueMap, TreeMap<String, Double> boatPositionMap){
        this.sensorsValueMap = sensorsValueMap;
        this.boatPositionMap = boatPositionMap;
    }

    @Override
    public void onResponse(JSONObject response) {
        double readDO = -1, readPH = -1, readEC = -1;
        double boatLatitude = -1, boatLongitude = -1, boatRotation = -1;


        JSONObject sensors = null;
        try {
            sensors = response.getJSONObject("sensors");
            readDO = sensors.getDouble("do");
            readEC = sensors.getDouble("ec");
            readPH = sensors.getDouble("ph");

            JSONObject boatPositionRead = response.getJSONObject("GPS");
            boatLatitude = boatPositionRead.getDouble("lat");
            boatLongitude = boatPositionRead.getDouble("lng");
            //speed = boatPositionRead.getDouble("speed");

            JSONObject boatAPS = response.getJSONObject("APS");
            boatRotation = boatAPS.getDouble("heading");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sensorsValueMap.put("PH", readPH);
        sensorsValueMap.put("DO", readEC);
        sensorsValueMap.put("EC", readDO);

        boatPositionMap.put("Ltd", boatLatitude);
        boatPositionMap.put("Lng", boatLongitude);
        boatPositionMap.put("Rot", boatRotation);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        sensorsValueMap.put("PH", -1.0);
        sensorsValueMap.put("DO", -1.0);
        sensorsValueMap.put("EC", -1.0);

        boatPositionMap.put("Ltd", -1.0);
        boatPositionMap.put("Lng", -1.0);
        boatPositionMap.put("Rot", -1.0);
    }
}
