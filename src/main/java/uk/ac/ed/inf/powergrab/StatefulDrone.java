package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class StatefulDrone extends Drone{
	

	public Feature closestFeature;
	public Feature closestLighthouse;
	public List<Feature> lighthouseList= new ArrayList<Feature>();//these are the stations that need to be visited
	public List<Feature> dangerList = new ArrayList<Feature>();//these are the stations that need to be avoided
	private HashMap<Direction,Double> distancesFromStation = new HashMap<Direction,Double>();
	
	public StatefulDrone(Position startposition, int userSeed, String mapSource,String newFileName) {
		this.startposition=startposition;
		this.userSeed=userSeed;
		this.power=250;
		this.mapSource=mapSource;
		this.newFileName=newFileName;
	}
	
	public void run() {
		setUpDrone();
		for (Feature f : fc.features()) {//puts all the lighthouses and danger stations in separate lists
			String symbol = (f.getStringProperty("marker-symbol"));
			if(symbol.equals("lighthouse")) {
				lighthouseList.add(f);
			}
			if(symbol.equals("danger")) {
				dangerList.add(f);
			}
		}
		closestLighthouse = findClosestPositiveStation();
		while (power>0 && moves<=250) {
			for (Direction dir : Direction.values()) {
				Position maybeNextPosition = currentposition.nextPosition(dir);
				Point stationPoint = (Point) closestLighthouse.geometry();
				double stationLatitude = (stationPoint.coordinates().get(1));//-3 ones are latitude
				double stationLongitude = (stationPoint.coordinates().get(0));//-55 ones are longitude
				double distanceToStation = Math.sqrt((Math.pow((maybeNextPosition.latitude-stationLatitude), 2))+(Math.pow((maybeNextPosition.longitude-stationLongitude), 2)));
				distancesFromStation.put(dir, distanceToStation);
			}
			//System.out.println(distancesFromStation);
			Direction bestdirection=null;
			double lowestDistance=1000;
			for (Direction dir : Direction.values()) {
				if (distancesFromStation.get(dir)<lowestDistance) {
					int danger=0;
					for (Feature f : dangerList) {//checking if the best direction would put us in range of danger
						Geometry stationgeometry = (Geometry) f.geometry();
		        			Point stationpoint = (Point) stationgeometry;
						double currentDistance = Math.sqrt((Math.pow((stationpoint.latitude()-currentposition.nextPosition(dir).latitude), 2))+(Math.pow((stationpoint.longitude()-currentposition.nextPosition(dir).longitude), 2)));
						if (currentDistance<0.00025) {
							danger=1;
							//System.out.println("This direction is too dangerous!");
						}
					}
					if (danger==0) {//if the direction is not dangerous then it's possible that it is the best direction
						lowestDistance=distancesFromStation.get(dir);
						bestdirection=dir;
					}
				}
			}
			moveDrone(bestdirection);
			//now it's the time to investigate if we have got coins or not and if we got the coins, drain the station and choose a new station to focus on
			HashMap<Feature,Double> stationsInRange = new HashMap<Feature,Double>();
			for (Feature f :lighthouseList) {//Makes list of features in range
				Geometry currentgeometry = (Geometry) f.geometry();
    				Point currentpoint = (Point) currentgeometry;
    				double currentDistance = Math.sqrt((Math.pow((currentpoint.latitude()-currentposition.latitude), 2))+(Math.pow((currentpoint.longitude()-currentposition.longitude), 2)));
    				if (currentDistance<0.00025) {//the station feature is reachable from the nextPosition
    					stationsInRange.put(f, currentDistance);
    				}
			}
			//At this point stationsInRange has all the stations in range of the current position and the closest station will be drained.
			double lowestStationDistance=1000;
			Feature closestStationInRange=null;
			for (Entry<Feature, Double> f: stationsInRange.entrySet()) {//gets the station closest in range
				if(f.getValue()<lowestStationDistance) {
					lowestStationDistance=f.getValue();
					closestStationInRange=f.getKey();
				}
			}
			if (closestStationInRange!=null) {//we have a station that's being affected
				coins=coins+closestStationInRange.getProperty("coins").getAsDouble();
				power=power+closestStationInRange.getProperty("power").getAsDouble();
				drainStation(closestStationInRange);
				String symbol = (closestStationInRange.getStringProperty("marker-symbol"));
				if (symbol.equals("lighthouse")) {
					lighthouseList.remove(closestStationInRange);
				}
				closestLighthouse = findClosestPositiveStation();
			}
		}
		writer.close();//at this point the stateless drone's journey is complete
		makeNewGeoJson();
	
	}
	
	
	Feature findClosestPositiveStation() {
		double lowestDistance=1000;
		for (Feature f : lighthouseList) {//for a direction go through every feature and find the one with the shortest distance
			Point stationPoint = (Point) f.geometry();
			double stationLatitude = (stationPoint.coordinates().get(1));//-3 ones are latitude
			double stationLongitude = (stationPoint.coordinates().get(0));//-55 ones are longitude
			double currentDistance = Math.sqrt((Math.pow((currentposition.latitude-stationLatitude), 2))+(Math.pow((currentposition.longitude-stationLongitude), 2)));
			if (currentDistance<lowestDistance) {
					closestFeature=f;
					lowestDistance=currentDistance;
			}
		}
		System.out.println(closestFeature);
		return closestFeature;
	}
	
	
}
	
	
	
	
	