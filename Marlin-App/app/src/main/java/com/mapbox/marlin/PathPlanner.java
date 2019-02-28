package com.mapbox.marlin;

import android.os.Debug;
import android.util.Log;

//import com.amazonaws.RequestClientOptions;
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

    public  ArrayList<Marker> getStandardPath(){
        ArrayList<Marker> toReturn = new ArrayList<>();
        for (Marker m : markerArrayList)
            toReturn.add(m);
        return toReturn;
    }

    public ArrayList<Marker> getSpiralPath(int n){
        ArrayList<LatLng> pointList = new ArrayList<>();

        for (Marker m : markerArrayList)
            pointList.add(m.getPosition());

        sortByX(pointList);

        ArrayList<LatLng> perimeterList = new ArrayList<>();
        perimeterList.add(pointList.get(0));
        double offset = 0;

        // Find the perimeter of the points
        for (int i = 0; i < pointList.size()-1; i++) {
            ArrayList<Double> angles = new ArrayList<>();
            for (LatLng point : pointList) {
                double angle = getAngle(perimeterList.get(perimeterList.size()-1), point, offset);
                angles.add(angle);
            }
            offset = Collections.min(angles);
            int minIndex = angles.indexOf(offset);
            if (perimeterList.contains(pointList.get(minIndex))) break;
            perimeterList.add(pointList.get(minIndex));
        }
        perimeterList = sortByIndex(perimeterList);

        // Spiral Generation phase

        // Distance from centroid for every point
        ArrayList<LatLng> spiralList = new ArrayList<>();
        LatLng centroid = getCentroid(perimeterList);

        ArrayList<Double> stepList = new ArrayList<>();
        ArrayList<Double> distanceList = new ArrayList<>();
        for (int i=0; i < perimeterList.size(); i++){
            double step = (getDistance(centroid, perimeterList.get(i)) / n);
            stepList.add(step);
            distanceList.add(step);
        }

        for (int i = 0; i < n-1; i++){
            for(int k=0; k < perimeterList.size(); k++){
                LatLng point = perimeterList.get(k);
                double angle = getAngle(point, centroid, 0);
                double newX = point.getLatitude() + distanceList.get(k) * Math.cos(Math.toRadians(angle));
                double newY = point.getLongitude() + distanceList.get(k) * Math.sin(Math.toRadians(angle));

                LatLng newPoint = new LatLng();
                newPoint.setLatitude(newX);
                newPoint.setLongitude(newY);

                spiralList.add(newPoint);
                distanceList.set(k, distanceList.get(k) + stepList.get(k));
            }
        }

        // Generate the marker list from the point list
        perimeterList.addAll(spiralList);
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
        angle -= offset;
        if(angle < 0) angle += 360;
        if(angle == 0) angle = 360;
        return angle;
    }

    private double getDistance(LatLng a, LatLng b){
        return Math.sqrt(Math.pow(b.getLatitude() - a.getLatitude(), 2) + Math.pow(b.getLongitude() - a.getLongitude(),2));
    }

    private double getArea(ArrayList<LatLng> pointList){
        double area = 0;

        area += (pointList.get(pointList.size()-1).getLatitude() * pointList.get(0).getLongitude()) - (pointList.get(0).getLatitude() * pointList.get(pointList.size()-1).getLongitude());
        for (int i = 0; i < pointList.size()-1; i++)
            area += (pointList.get(i).getLatitude() * pointList.get(i+1).getLongitude()) - (pointList.get(i+1).getLatitude() * pointList.get(i).getLongitude());

        area *= 0.5;
        return area;
    }

    private LatLng getCentroid(ArrayList<LatLng> pointList){
        double area = getArea(pointList);

        double x = 0;
        double y = 0;

        x += ((pointList.get(pointList.size()-1).getLatitude() + pointList.get(0).getLatitude()) * (pointList.get(pointList.size()-1).getLatitude()*pointList.get(0).getLongitude()-pointList.get(0).getLatitude()*pointList.get(pointList.size()-1).getLongitude()));
        y += ((pointList.get(pointList.size()-1).getLongitude() + pointList.get(0).getLongitude()) * (pointList.get(pointList.size()-1).getLatitude()*pointList.get(0).getLongitude()-pointList.get(0).getLatitude()*pointList.get(pointList.size()-1).getLongitude()));

        for (int i = 0; i < pointList.size()-1; i++){
            x += ((pointList.get(i).getLatitude() + pointList.get(i+1).getLatitude()) * (pointList.get(i).getLatitude()*pointList.get(i+1).getLongitude()-pointList.get(i+1).getLatitude()*pointList.get(i).getLongitude()));
            y += ((pointList.get(i).getLongitude() + pointList.get(i+1).getLongitude()) * (pointList.get(i).getLatitude()*pointList.get(i+1).getLongitude()-pointList.get(i+1).getLatitude()*pointList.get(i).getLongitude()));
        }

        x *= (1/(6*area));
        y *= (1/(6*area));

        LatLng toReturn = new LatLng();
        toReturn.setLatitude(x);
        toReturn.setLongitude(y);
        return toReturn;
    }

    private void sortByX(ArrayList<LatLng> pointList){
        Collections.sort(pointList, new Comparator<LatLng>() {
            @Override
            public int compare(LatLng a, LatLng b)
            {
                if (a.getLongitude() > b.getLongitude()) return 1;
                else return -1;
            }
        });
    }

    private ArrayList<LatLng> sortByIndex(ArrayList<LatLng> perimeterList){
        ArrayList<LatLng> originalList = new ArrayList<>();

        for (Marker m : markerArrayList)
            originalList.add(m.getPosition());

        int startingIndex = 0;
        for (int i = 0; i < originalList.size(); i++){
            if(perimeterList.contains(originalList.get(i))) {
                startingIndex = perimeterList.indexOf(originalList.get(i));
                break;
            }
        }

        ArrayList<LatLng> toReturn = new ArrayList<>();
        for (int i = 0; i < perimeterList.size(); i++ ){
            toReturn.add(perimeterList.get(startingIndex));
            startingIndex ++;
            if(startingIndex > perimeterList.size() - 1)
                startingIndex = 0;
        }

        return  toReturn;
    }

}
