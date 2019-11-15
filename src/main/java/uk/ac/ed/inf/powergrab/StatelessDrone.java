package uk.ac.ed.inf.powergrab;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.HashMap;
//import java.util.List;
import java.util.Random;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
//import com.mapbox.geojson.GeoJson;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class StatelessDrone {
	
	public Position startposition;
	public int userSeed;
	public double coins;
	public double power;
	public Position currentposition;
	String geojsonFileName;

	public int moves;//everytime i make move position i'll increment this
	private double highestCoins;
	
	public StatelessDrone(Position startposition, int userSeed) {
		this.startposition=startposition;
		this.userSeed=userSeed;
		this.power=250;
	}

	public void run(String mapSource, String newFileName) {
		PrintWriter writer = null;
		geojsonFileName = (newFileName.substring(0, newFileName.length() - 3))+"geojson";
		try {
			writer = new PrintWriter(newFileName, "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    		Random rand = new Random(userSeed);
		FeatureCollection fc =FeatureCollection.fromJson(mapSource);
		Point firstPoint = Point.fromLngLat(startposition.longitude, startposition.latitude);
		ArrayList<Point> lineStringPoints = new ArrayList<Point>();
		lineStringPoints.add(0, firstPoint);
		//Point firstPoint = builder.f
		//mylinestring.coordinates().add(e)
		moves=0;
		currentposition=startposition;
		
		while (power>0 && moves<=250) {//this is for each move
			HashMap<Direction,Feature> visitablePoints = new HashMap<Direction, Feature>();//each direction will have one feature (station)
			searchForStations(fc,visitablePoints);
			//at this stage the drone can't see any stations from its current position
	        if (visitablePoints.keySet().isEmpty()) {//time to pick randomly between the 16 directions and make the move
	        		makeRandomMove(rand,writer,lineStringPoints,moves);
	        }
	        
			if(!(visitablePoints.keySet().isEmpty())) {//there's directions we can visit
				System.out.println(visitablePoints.keySet());
				Direction bestdirection = null;
				highestCoins = -1000;
				bestdirection = findBestDirection(visitablePoints,bestdirection);
				//Right now, I pick the first direction that gives highest coins. I don't care if it's not the closest I get to the station because accuracy doesn't matter.
				//Maybe I should care because a better drone would be closer?
				System.out.println(bestdirection);
				System.out.println(highestCoins);
				
				//This is if the drone only has bad options (losing coins or lighthouses that have already been used)
				if (highestCoins<=0) {//we should pick a random direction that is not one of the visitable points directions
					makeNoCoinGainMove(visitablePoints,lineStringPoints,rand,writer);
				}
				
				//At this point I know where I want to move and the consequence is positive
				else if (highestCoins>0){
					makeCoinGainMove(visitablePoints,moves,bestdirection,fc,writer,lineStringPoints);
				}
			}
			
		}
		writer.close();
		makeNewGeoJson(lineStringPoints,fc);
		//at this point the stateless drone's journey is complete
		System.out.println(currentposition.latitude);
		System.out.println(currentposition.longitude);
		System.out.println(power);
		System.out.println(coins);
		System.out.println(moves);
//		System.out.println(power);
//		System.out.println(moves);
	}
	

	private void makeNoCoinGainMove(HashMap<Direction, Feature> visitablePoints, ArrayList<Point> lineStringPoints, Random rand, PrintWriter writer) {
		//Visitable points take away any feature with negative coin!
		for (Direction d : Direction.values()) {//Here I take lighthouses out of visitable points so that a lighthouse can be visited even if it has 0 coins
			Feature f = visitablePoints.get(d);
			if (f!=null) {
				String symbol = (f.getStringProperty("marker-symbol"));
				if (symbol.equals("lighthouse")) {
					visitablePoints.remove(d);
				}
			}
		}	
		System.out.println("RANDOM MOVE BECAUSE HIGHEST COINS AVAILABLE IN CURRENT POSITION IS 0");
    		int random_integer = rand.nextInt(15);
        	Direction nextone = Direction.values()[random_integer];//0 is N ... and 15 is NNW
        	while ( (!(currentposition.nextPosition(nextone).inPlayArea())) || (visitablePoints.keySet().contains(nextone))  ) {//if it's next position not in play area and direction is not a bad direction with 0 or less coins
        		random_integer = rand.nextInt(15);
	        	nextone = Direction.values()[random_integer];
        	}
        	power=power-1.25;//decrease power
        	writer.println(currentposition.latitude+","+currentposition.longitude+","+nextone+","+currentposition.nextPosition(nextone).latitude+","+currentposition.nextPosition(nextone).longitude+","+coins+","+power);
        	currentposition = currentposition.nextPosition(nextone);
        	//NEED TO MAKE A POINT AND ADD TO LINESTRING POINTS HERE
        	Point newPoint = Point.fromLngLat(currentposition.longitude, currentposition.latitude);
    		lineStringPoints.add(lineStringPoints.size()-1, newPoint);
        	moves++;//increment moves by one
        	System.out.println("coins after move:");
        	System.out.println(coins);
		
	}

	private void makeNewGeoJson(ArrayList<Point> lineStringPoints, FeatureCollection fc) {
		LineString mylinestring =  LineString.fromLngLats(lineStringPoints);//this is the drone's path
		Feature linestringFeature = Feature.fromGeometry(mylinestring);
		fc.features().add(linestringFeature);
		//System.out.println(fc.features());
		
		//System.out.println(mapSource);
		System.out.println(fc.toJson());
		BufferedWriter writer2 = null;//writing the geojson file with the lineString feature added.
		try {
			writer2 = new BufferedWriter(new FileWriter(geojsonFileName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			writer2.write(fc.toJson());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	     
	    try {
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void makeCoinGainMove(HashMap<Direction, Feature> visitablePoints, int moves, Direction bestdirection, FeatureCollection fc, PrintWriter writer, ArrayList<Point> lineStringPoints) {
		coins=coins+visitablePoints.get(bestdirection).getProperty("coins").getAsDouble();
		power=power+visitablePoints.get(bestdirection).getProperty("power").getAsDouble();
		power=power-1.25;
		Position nextPosition = currentposition.nextPosition(bestdirection);
		writer.println(currentposition.latitude+","+currentposition.longitude+","+bestdirection+","+nextPosition.latitude+","+nextPosition.longitude+","+coins+","+power);
		currentposition=nextPosition;
		Point newPoint = Point.fromLngLat(currentposition.longitude, currentposition.latitude);
			lineStringPoints.add(lineStringPoints.size()-1, newPoint);
		moves++;
		System.out.println("coins after move:");
		System.out.println(coins);
		//next I need to change that feature's coins and power, this doesn't work yet
		Feature featureVisited = visitablePoints.get(bestdirection);
		System.out.println(fc.features().contains(featureVisited));
		fc.features().remove(featureVisited);//take the feature out of the feature collection to replace it with modified version
		featureVisited.removeProperty("coins");
		featureVisited.removeProperty("power");
		featureVisited.addNumberProperty("coins", 0);
		featureVisited.addNumberProperty("power", 0);
		fc.features().add((fc.features().size()-1), featureVisited);//adding feature back into feature collection
		
	}

	private void searchForStations(FeatureCollection fc, HashMap<Direction, Feature> visitablePoints) {
		for (Direction dir : Direction.values()) {
			Position maybeNextPosition = currentposition.nextPosition(dir);
			double lowestDistance = -1;
			for (Feature f : fc.features()) {//for a direction go through every feature and find the one with the shortest distance
				Geometry currentgeometry = (Geometry) f.geometry();
        			Point currentpoint = (Point) currentgeometry;
        			//double stationCoins = f.getProperty("coins").getAsDouble();
				double currentDistance = Math.sqrt((Math.pow((currentpoint.latitude()-maybeNextPosition.latitude), 2))+(Math.pow((currentpoint.longitude()-maybeNextPosition.longitude), 2)));
				if (currentDistance<0.00025) {//the station feature is reachable from the nextPosition
					if (lowestDistance==-1) {//first time we have gotten a distance
						lowestDistance=currentDistance;
						visitablePoints.put(dir, f);
					}
					else {//there's already a feature
						if (currentDistance<lowestDistance) {//check if the new distance is lower and if it is, it's our new feature
							visitablePoints.remove(dir);
							lowestDistance=currentDistance;
							visitablePoints.put(dir, f);
							
						}
					}
				}
			}
		}
	}
	
	
	private void makeRandomMove(Random rand, PrintWriter writer, ArrayList<Point> lineStringPoints, int moves) {
        System.out.println("RANDOM MOVE BECAUSE NO VISTABLE POINTS");
		int random_integer = rand.nextInt(15);
	    	Direction nextone = Direction.values()[random_integer];//0 is N ... and 15 is NNW
	    	while (!(currentposition.nextPosition(nextone).inPlayArea())) {//if it's next position not in play area
	    		random_integer = rand.nextInt(15);
	        	nextone = Direction.values()[random_integer];
	    	}
	    	power=power-1.25;//decrease power
	    	writer.println(currentposition.latitude+","+currentposition.longitude+","+nextone+","+currentposition.nextPosition(nextone).latitude+","+currentposition.nextPosition(nextone).longitude+","+coins+","+power);
	    	currentposition = currentposition.nextPosition(nextone);
	    	//NEED TO MAKE A POINT AND ADD TO LINESTRING POINTS HERE
	    	Point newPoint = Point.fromLngLat(currentposition.longitude, currentposition.latitude);
			lineStringPoints.add(lineStringPoints.size()-1, newPoint);
	    	moves++;//increment moves by one
	    	System.out.println("coins after move:");
	    	System.out.println(coins);
	}
	
	private Direction findBestDirection(HashMap<Direction, Feature> visitablePoints, Direction bestdirection) {
		for (Direction d : visitablePoints.keySet()) {//find the directions that give max number of coins
			Feature f = visitablePoints.get(d);
			double stationCoins = f.getProperty("coins").getAsDouble();
			if (highestCoins==-1000) {//if it's the first station set initialise its coins to be the highest
				highestCoins=stationCoins;
				bestdirection=d;
			}
			else {
				System.out.println(d);
				if (stationCoins>highestCoins) {
					highestCoins=stationCoins;
					bestdirection=d;
				}
			}
		}
		return bestdirection;
		
	}

}
