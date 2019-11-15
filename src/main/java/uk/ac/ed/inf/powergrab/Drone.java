package uk.ac.ed.inf.powergrab;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;


public class Drone {

	public double coins;
	public double power;
	public int moves;//everytime i make move position i'll increment this
	public Position currentposition;
	public Position startposition;
	public int userSeed;
	String geojsonFileName;
	PrintWriter writer;
	public Random rand;
	public FeatureCollection fc;
	public ArrayList<Point> lineStringPoints;
	public String mapSource;
	public String newFileName;

	public void makeNewGeoJson() {
		LineString mylinestring =  LineString.fromLngLats(lineStringPoints);//this is the drone's path
		Feature linestringFeature = Feature.fromGeometry(mylinestring);
		fc.features().add(linestringFeature);
		BufferedWriter writer2 = null;//writing the geojson file with the lineString feature added.
		try {
			writer2 = new BufferedWriter(new FileWriter(geojsonFileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    try {
			writer2.write(fc.toJson());
		} catch (IOException e) {
			e.printStackTrace();
		}
	    try {
			writer2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(coins);
		System.out.println(power);
	}
	
	public void moveDrone(Direction nextone) {
	    	power=power-1.25;//decrease power
	    	writer.println(currentposition.latitude+","+currentposition.longitude+","+nextone+","+currentposition.nextPosition(nextone).latitude+","+currentposition.nextPosition(nextone).longitude+","+coins+","+power);
	    	currentposition = currentposition.nextPosition(nextone);
	    	Point newPoint = Point.fromLngLat(currentposition.longitude, currentposition.latitude);
		lineStringPoints.add(lineStringPoints.size()-1, newPoint);
	    	moves++;//increment moves by one
	}
	
	public void setUpDrone() {
		geojsonFileName = (newFileName.substring(0, newFileName.length() - 3))+"geojson";
		try {
			writer = new PrintWriter(newFileName, "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    		rand = new Random(userSeed);
		fc =FeatureCollection.fromJson(mapSource);
		Point firstPoint = Point.fromLngLat(startposition.longitude, startposition.latitude);
		lineStringPoints = new ArrayList<Point>();
		lineStringPoints.add(0, firstPoint);
		moves=0;
		currentposition=startposition;
	}
	
	public void drainStation(Feature featureVisited) {
		fc.features().remove(featureVisited);//take the feature out of the feature collection to replace it with modified version
		featureVisited.removeProperty("coins");
		featureVisited.removeProperty("power");
		featureVisited.addNumberProperty("coins", 0);
		featureVisited.addNumberProperty("power", 0);
		fc.features().add((fc.features().size()-1), featureVisited);//adding feature back into feature collection
	}
}
