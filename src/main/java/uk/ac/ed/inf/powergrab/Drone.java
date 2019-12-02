package uk.ac.ed.inf.powergrab;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;


public class Drone {

	public BigDecimal coins;
	public BigDecimal power;
	public int moves;
	public Position currentPosition;
	public Position startPosition;
	public int userSeed;
	private String geojsonFileName;
	public PrintWriter writer;
	public Random rand;
	public FeatureCollection fc;
	private ArrayList<Point> lineStringPoints;
	public String mapSource;
	public String newFileName;

	public void makeNewGeoJson() {
		LineString mylinestring =  LineString.fromLngLats(lineStringPoints);//this is the drone's flight path
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
		System.out.println("Coins: "+coins.doubleValue());
		System.out.println("Power: "+power.doubleValue());
	}
	
	public void moveDrone(Direction nextone) {//moves the drone in given direction
	    	power=power.subtract(new BigDecimal(1.25));//each move costs 1.25 power
	    	writer.print(currentPosition.latitude+","+currentPosition.longitude+","+nextone+","+currentPosition.nextPosition(nextone).latitude+","+currentPosition.nextPosition(nextone).longitude+",");
	    	currentPosition = currentPosition.nextPosition(nextone);
	    	Point newPoint = Point.fromLngLat(currentPosition.longitude, currentPosition.latitude);
		lineStringPoints.add(newPoint);//adds move to the flight path line
	    	moves++;
		if (!currentPosition.inPlayArea()) {//this allows me to quickly check if drone ever goes out of map as it will throw exception
				throw new RuntimeException("Drone went out of the play area.");
		}
	}
	
	public void setUpDrone() {//sets up drone at the start of it's flight
		geojsonFileName = (newFileName.substring(0, newFileName.length() - 3))+"geojson";
		try {
			writer = new PrintWriter(newFileName, "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    		rand = new Random(userSeed);
		fc =FeatureCollection.fromJson(mapSource);//contains all the station features of the map
		Point firstPoint = Point.fromLngLat(startPosition.longitude, startPosition.latitude);
		lineStringPoints = new ArrayList<Point>();
		lineStringPoints.add(firstPoint);
		moves=0;
		coins = new BigDecimal(0);
		currentPosition=startPosition;//drone is now in the correct starting position
	}
	
	public void drainStation(Feature featureVisited) {//used to reset a lighthouse after the drone has taken its coins and power
		fc.features().remove(featureVisited);//take the feature out of the feature collection to replace it with modified version
		featureVisited.removeProperty("coins");
		featureVisited.removeProperty("power");
		featureVisited.addNumberProperty("coins", 0);
		featureVisited.addNumberProperty("power", 0);
		fc.features().add((fc.features().size()-1), featureVisited);//adding feature back into feature collection
	}
	
	public void drainNegativeStation(Feature featureVisited) {//used to reset a danger station and change the drone's coins and power as a result
		BigDecimal stationCoins = new BigDecimal((featureVisited.getProperty("coins").getAsDouble()));
		BigDecimal stationPower = new BigDecimal(featureVisited.getProperty("power").getAsDouble());
		if(coins.add(stationCoins).doubleValue()<0) {//station will have coin debt
			coins=new BigDecimal(0);
			stationCoins = coins.add(stationCoins);
		}
		else {//drone has enough coins to lose so station stores no coin debt
			coins=coins.add(stationCoins);
			stationCoins=new BigDecimal(0);
		}
		if(power.add(stationPower).doubleValue()<0) {//station will have power debt
			power=new BigDecimal(0);
			stationPower=power.add(stationPower);
		}
		else {//drone has enough power to lose so station stores no power debt
			power=power.add(stationPower);
			stationPower = new BigDecimal(0);
		}
		fc.features().remove(featureVisited);//take the feature out of the feature collection to replace it with modified version
		featureVisited.removeProperty("coins");
		featureVisited.removeProperty("power");
		featureVisited.addNumberProperty("coins", stationCoins);
		featureVisited.addNumberProperty("power", stationPower);
		fc.features().add((fc.features().size()-1), featureVisited);//adding feature back into feature collection
	}
}
