package com.mapbox.marlin;

import android.util.Log;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class PathPlanner {

    private ArrayList<Marker> markerArrayList;

    public void setPoints(ArrayList<Marker> markerArrayList){
        this.markerArrayList = markerArrayList;
    }

    public ArrayList<Marker> getSpiralPath(){
        ArrayList<LatLng> pointList = new ArrayList<>();

        for (Marker m : markerArrayList)
            pointList.add(m.getPosition());

        //double angle = getAngle(pointList.get(0), pointList.get(1), 0);

        //Log.d("Path", ""+ angle);

        //Log.d("Path", pointList.toString());
        sortByX(pointList);
        //Log.d("Path", pointList.toString());

        ArrayList<LatLng> perimeterList = new ArrayList<>();
        perimeterList.add(pointList.get(0));

        ArrayList<Double> angles = new ArrayList<>();
        for (LatLng point : pointList) {
            double angle = getAngle(perimeterList.get(0), point, 0);
            angles.add(angle);
        }

        int minIndex = angles.indexOf(Collections.min(angles));
        perimeterList.add(pointList.get(minIndex));
        pointList.remove(minIndex);


        ArrayList<Marker> toReturn = new ArrayList<>();
        for (LatLng point : perimeterList){
            Marker newMarker = new Marker(new MarkerOptions()
                    .position(point)
                    .title("Marker #" + markerArrayList.size())
            );
            toReturn.add(newMarker);
        }

        return toReturn;
    }

    private double getAngle(LatLng a, LatLng b, double offset){
        double angle = Math.toDegrees(Math.atan2(b.getLongitude()-a.getLongitude(), b.getLatitude()-a.getLatitude()));
        angle += offset;
        if(angle < 0) angle += 360;
        return angle;
    }

    private void sortByX(ArrayList<LatLng> pointList){
        Collections.sort(pointList, new Comparator<LatLng>() {
            @Override
            public int compare(LatLng a, LatLng b)
            {
                if (a.getLatitude() > b.getLatitude()) return 1;
                else return -1;
            }
        });
    }

}
