package seng302;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created on 7/03/17.
 * Class to figure out the mark locations and degrees.
 */

public class Course {

    private ArrayList<CompoundMark> courseOrder;
    private ArrayList<Coordinate> boundary;
    private double minLat, minLon, maxLat, maxLon;
    private HashMap<String, CompoundMark> marks;
    private double windDirection;

    private static final double EARTH_RADIUS_IN_NAUTICAL_MILES = 3437.74677;

    public Course() {
        this.marks = new HashMap<>();
        this.courseOrder = new ArrayList<>();
        this.boundary = new ArrayList<>();
    }

    /**
     * Puts mark into the marks HashMap, with the name of the mark as the Key
     * @param mark - a defined Mark object
     */
    public void addNewMark(CompoundMark mark){
        marks.put(mark.getName(), mark);
    }

    /**
     * Appends a mark to the course order ArrayList. The mark must already exist in the marks HashMap
     * @param markName - the name of the mark to look up in the marks HashMap
     */
    public void addMarkInOrder(String markName){
        courseOrder.add(marks.get(markName));
    }

    /**
     * Appends a coordinate (lat/long) to the boundary list to make a polygon
     * @param coord The coordinate of a new boundary point
     */
    public void addToBoundary(Coordinate coord){
        boundary.add(coord);
    }
    /**
     * This function finds the distance between each mark on the course
     * @param markIndex1 - the index in the courseOrder array of the source mark
     * @param markIndex2 - the index in the courseOrder array of the destination mark
     * @return distance between source mark and destination mark in nautical miles
     */
    public double distanceBetweenMarks(int markIndex1, int markIndex2){
        CompoundMark mark1 = this.courseOrder.get(markIndex1);
        CompoundMark mark2 = this.courseOrder.get(markIndex2);
        double distance = greaterCircleDistance(mark1.getLat(), mark2.getLat(), mark1.getLon(), mark2.getLon());
        return distance;
    }

    /**
     * Calculates distance between two lat lon locations in nautical miles
     * @param lat1 - the latitude of the source point, in degrees
     * @param lon1 - the longitude of the source point, in degrees
     * @param lat2 - the latitude of the source point, in degrees
     * @param lon2 - the longitude of the source point, in degrees
     * @return distance from (lat1, lon1) to (lat2, lon2) in nautical miles
     */
    public static double greaterCircleDistance(double lat1, double lat2, double lon1, double lon2){
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);

        return EARTH_RADIUS_IN_NAUTICAL_MILES * Math.acos(Math.sin(lat1) * Math.sin(lat2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1));
    }

    /**
     * This function uses heading calculations to find the headings between two marks.
     * @param markIndex1 - the index in the courseOrder array of the source mark
     * @param markIndex2 - the index in the courseOrder array of the destination mark
     * @return heading - in degrees, from source mark to destination mark
     */
    public double headingsBetweenMarks(int markIndex1, int markIndex2){
        CompoundMark mark1 = this.courseOrder.get(markIndex1);
        CompoundMark mark2 = this.courseOrder.get(markIndex2);
        double Ldelta = Math.toRadians(mark2.getLon()) - Math.toRadians(mark1.getLon());
        double X = Math.cos(Math.toRadians(mark2.getLat())) * Math.sin(Ldelta);
        double Y = Math.cos(Math.toRadians(mark1.getLat())) * Math.sin(Math.toRadians(mark2.getLat()))
                - Math.sin(Math.toRadians(mark1.getLat())) * Math.cos(Math.toRadians(mark2.getLat())) * Math.cos(Ldelta);
        double heading = Math.toDegrees(Math.atan2(X, Y));
        if(heading < 0){
            heading += 360;
        }
        return heading;
    }

    public void updateMinMaxLatLon(double newLat, double newLon){
        if(newLat > maxLat) {
            maxLat = newLat;
        } else if(newLat < minLat) {
            minLat = newLat;
        }
        if(newLon > maxLon) {
            maxLon = newLon;
        } else if(newLon < minLon) {
            minLon = newLon;
        }
    }

    /**
     * This function grabs all the latitude and longitude points for each mark/gate
     * The furthest most points to the North and East are found and made the max latitude and longitude
     * The furthest most points to the South and West are also found and made the min latitude and longitude.
     * This is then added to an ArrayList for future use.
     */
    public void initCourseLatLon() {
        maxLat = minLat = this.courseOrder.get(0).getLat();
        maxLon = minLon = this.courseOrder.get(0).getLon();

        for(CompoundMark mark : courseOrder){
            updateMinMaxLatLon(mark.getLat(), mark.getLon());
        }
        for(Coordinate coord : boundary){
            updateMinMaxLatLon(coord.getLat(), coord.getLon());
        }
        //Adding padding of 0.004 to each coordinate to make sure the visual area is large enough
        minLat -= 0.004; minLon -= 0.004; maxLat += 0.004; maxLon += 0.004;
    }

    /**
     * Getter for the course order
     */
    public ArrayList<CompoundMark> getCourseOrder(){
        return this.courseOrder;
    }

    public HashMap<String, CompoundMark> getMarks() {
        return marks;
    }


    public double getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(double windDirection) {
        this.windDirection = (windDirection + 360) % 360;
    }

    public double getMinLat() {
        return minLat;
    }

    public double getMinLon() {
        return minLon;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public ArrayList<Coordinate> getBoundary() {
        return boundary;
    }
}
