package uk.ac.ed.inf.powergrab;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class StatefulDrone extends Drone{

	private Feature closestFeature;
	private Feature closestLighthouse;
	private List<Feature> lighthouseList= new ArrayList<Feature>();//these are the stations that need to be visited
	private List<Feature> dangerList = new ArrayList<Feature>();//these are the stations that need to be avoided
	private HashMap<Direction,Double> distancesFromStation = new HashMap<Direction,Double>();
	private Direction oppositeDirection;
	private List<Direction> oppositeDirections = new ArrayList<Direction>();
	private int movesSinceCoinGain;
	private int backTrackAttempt=0;

	public StatefulDrone(Position startposition, int userSeed, String mapSource,String newFileName) {
		this.startPosition=startposition;
		this.userSeed=userSeed;
		this.power=new BigDecimal(250);
		this.mapSource=mapSource;
		this.newFileName=newFileName;
	}

	public void run() {//starts the drone's gameplay
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
		closestLighthouse = findClosestPositiveStation();//this is the initial goal of the drone
		while (power.doubleValue()>0 && moves<250) {
			findDistancesFromStation();
			Direction bestDirection = findBestDirection();
			if (movesSinceCoinGain>=30 && lighthouseList.size()>0) {//Fail safe in case we aren't making any progress
				if (lighthouseList.size()>1){//this means we can pick a different lighthouse as the goal and attempt to reach the current lighthouse again later
					Feature oldClosestLighthouse = closestLighthouse;
					lighthouseList.remove(oldClosestLighthouse);
					closestLighthouse = (lighthouseList.get(rand.nextInt(lighthouseList.size())));
					lighthouseList.add(oldClosestLighthouse);
					findDistancesFromStation();
					bestDirection = findBestDirection();
					movesSinceCoinGain=0;//gives the drone at least 30 more moves to find the new goal station
				}
				else{//time to backtrack in order to reach final lighthouse. if backtracking doesn't work, the drone may move through danger if the lighthouse gives more coins than the danger loses
					bestDirection = noProgressDirection(bestDirection);
				}
			}
			moveDrone(bestDirection);//now it's the time to investigate if we have got coins or not and if we got the coins, drain the station and choose a new station to focus on
			oppositeDirections.add(oppositeDirection);
			Feature closestStationInRange = checkIfStationInRange();
			if (closestStationInRange!=null) {//not null means a station is in range and we need to take it's effects
				takeStationEffect(closestStationInRange);
			}
			movesSinceCoinGain++;
			writer.println(coins.doubleValue()+","+power.doubleValue());
		}
		writer.close();//at this point the stateful drone's journey is complete and moves and GeoJSON files are produced
		makeNewGeoJson();
	}

	private Direction noProgressDirection(Direction bestDirection) {//failsafe way of choosing direction when the only way the drone can reach the final station is by entering danger
		if (backTrackAttempt==0) {//the drone can make one backtrack attempt to become unstuck
			int currentMoves = 0;
			if (movesSinceCoinGain<30) {//we have just become stuck for last lighthouse
				while (currentMoves<20 && moves<250 && power.doubleValue()>0) {//back track 20 moves to attempt to get unstuck
					moveDrone(oppositeDirections.get(oppositeDirections.size()-1));
					currentMoves++;
					oppositeDirections.remove(oppositeDirections.size()-1);
				}
			}
			else if (movesSinceCoinGain>=30) {//we have been stuck in a loop trying to get to last lighthouse.
				while (currentMoves<40 && moves<250 && power.doubleValue()>0) {//back track 40 moves to backtrack through loop and get unstuck
					moveDrone(oppositeDirections.get(oppositeDirections.size()-1));
					currentMoves++;
					oppositeDirections.remove(oppositeDirections.size()-1);
				}
			}
			backTrackAttempt=1;
			bestDirection=findBestDirection();//after back tracking, reattempt to get to station
			return bestDirection;
		}
		else {//after a backtrack attempt has been made and the drone is stuck from getting the final lighthouse again, the drone determines if the goal is worth moving to
			double lowestDistance=1000;
			Direction newbest = null;
			for (Direction dir : Direction.values()) {//chooses direction closest to station, regardless of danger.
				if (distancesFromStation.get(dir)<lowestDistance) {
					if (currentPosition.nextPosition(dir).inPlayArea() && !(dir.equals(oppositeDirection))) {
						lowestDistance=distancesFromStation.get(dir);
						newbest=dir;
					}
				}
			}
			double dangerDistance=1000;
			for (Feature myF : fc.features()) {//for a direction go through every feature and find the one with the shortest distance
				Point stationPoint = (Point) myF.geometry();
				double stationLatitude = (stationPoint.coordinates().get(1));
				double stationLongitude = (stationPoint.coordinates().get(0));
				double currentdangerDistance = Math.sqrt((Math.pow((currentPosition.nextPosition(newbest).latitude-stationLatitude), 2))+(Math.pow((currentPosition.nextPosition(newbest).longitude-stationLongitude), 2)));
				if (currentdangerDistance<dangerDistance) {
					closestFeature=myF;
					dangerDistance=currentdangerDistance;
				}
			}
			if(closestLighthouse.getProperty("coins").getAsDouble()+closestFeature.getProperty("coins").getAsDouble()<0) {//only goes through red if the reward is worth the risk.
				lighthouseList.remove(closestLighthouse);
				bestDirection=findBestDirection();
			}
			else {
				bestDirection=newbest;//if the risk of danger was outweighed by the ligthouse's reward, the drone moves towards danger
			}
			return bestDirection;
		}
	}

	private void takeStationEffect(Feature closestStationInRange) {//checks if station is lighthouse or danger and alters values of drone and station
		BigDecimal stationCoins = new BigDecimal(closestStationInRange.getProperty("coins").getAsDouble());
		BigDecimal stationPower = new BigDecimal(closestStationInRange.getProperty("power").getAsDouble());
		if (coins.add(stationCoins).doubleValue()<coins.doubleValue() || power.add(stationPower).doubleValue()<power.doubleValue()) {//checking for negative station
			drainNegativeStation(closestStationInRange);
		}
		else {//we've hit a positive station
			double previousCoins = coins.doubleValue();
			coins=coins.add(stationCoins);
			power=power.add(stationPower);
			drainStation(closestStationInRange);
			if (coins.doubleValue()>previousCoins) {//means the drone has just encountered a lighthouse for the first time so that lighthouse can be removed from search list
				movesSinceCoinGain=0;
				lighthouseList.remove(closestStationInRange);
			}
		}
		if (lighthouseList.size()>0) {//determines new goal if there are lighthouses the drone has not yet visited
			closestLighthouse = findClosestPositiveStation();
		}
	}

	private Feature findClosestPositiveStation() {//calculates which lighthouse is closest to drone's current position and returns that station feature
		double lowestDistance=1000;
		for (Feature f : lighthouseList) {//for a direction go through every feature and find the one with the shortest distance
			Point stationPoint = (Point) f.geometry();
			double stationLatitude = (stationPoint.coordinates().get(1));//approx -3 values are latitude
			double stationLongitude = (stationPoint.coordinates().get(0));//approx -55 ones are longitude
			double currentDistance = Math.sqrt((Math.pow((currentPosition.latitude-stationLatitude), 2))+(Math.pow((currentPosition.longitude-stationLongitude), 2)));
			if (currentDistance<lowestDistance) {
				closestFeature=f;
				lowestDistance=currentDistance;
			}
		}
		return closestFeature;
	}

	private Direction findBestDirection() {//finds the next direction for the drone to move in
		Direction bestDirection=null;
		double lowestDistance=1000;
		for (Direction dir : Direction.values()) {
			if (distancesFromStation.get(dir)<lowestDistance) {
				int danger = dangerCheck(dir);
				if (danger==0 && currentPosition.nextPosition(dir).inPlayArea() && !(dir.equals(oppositeDirection))) {//if the direction is not dangerous and in play area then it's possible that it is the best direction
					lowestDistance=distancesFromStation.get(dir);//I don't allow opposite direction to prevent us from going back and forth over and over
					bestDirection=dir;
				}
				if (lighthouseList.size()==0 && danger==0 && currentPosition.nextPosition(dir).inPlayArea()) {//this is to allow the drone to move back and forth on the last lighthouse only
					lowestDistance=distancesFromStation.get(dir);
					bestDirection=dir;
				}
			}
		}
		if (bestDirection==null){//There is no ideal direction whatsoever e.g trapped in a red circle
			if (lighthouseList.size()>1){//this means we can pick a different lighthouse as the goal
				closestLighthouse = (lighthouseList.get(rand.nextInt(lighthouseList.size())));
				findDistancesFromStation();
				bestDirection=oppositeDirection;
			}
			else {//we are stuck trying to reach the last lighthouse
				lowestDistance=1000;//we are going to go in direction closest to station in play area even if it is dangerous
				for (Direction d : Direction.values()) {
					if (distancesFromStation.get(d)<lowestDistance) {
						if (currentPosition.nextPosition(d).inPlayArea() && !(d.equals(oppositeDirection))) {//if the direction is not dangerous and in play area then it's possible that it is the best direction
							lowestDistance=distancesFromStation.get(d);
							bestDirection=d;
						}
					}
				}
				bestDirection=noProgressDirection(bestDirection);//the drone will only move in the direction of danger to the goal if the gain from the lighthouse outweights the loss from the danger
			}

		}
		if (Direction.valueOf(bestDirection.toString()).ordinal()<8) {//getting the opposite direction of the previous move to avoid going backwards
			int compassPoint = Direction.valueOf(bestDirection.toString()).ordinal()+8;
			oppositeDirection = Direction.values()[compassPoint];
		}
		else {
			int compassPoint = Direction.valueOf(bestDirection.toString()).ordinal()-8;
			oppositeDirection = Direction.values()[compassPoint];
		}
		return bestDirection;
	}

	private int dangerCheck(Direction dir) {//checks if direction given will put the drone in danger
		int danger=0;//0 for no danger, 1 for danger
		for (Feature f : dangerList) {//checking if the best direction would put us in range of danger
			Geometry stationgeometry = (Geometry) f.geometry();
			Point stationpoint = (Point) stationgeometry;
			double currentDistance = Math.sqrt((Math.pow((stationpoint.latitude()-currentPosition.nextPosition(dir).latitude), 2))+(Math.pow((stationpoint.longitude()-currentPosition.nextPosition(dir).longitude), 2)));
			if (currentDistance<0.00025) {//in range of danger station
				danger=1;
			}
			if (danger==1) {//checks if the position is actually closest to a lighthouse in which case the direction is not ideal but not dangerous either
				Double min = Collections.min(distancesFromStation.values());
				if(min.equals(distancesFromStation.get(dir))) {//direction is the closest one to the lighthouse but is potentially dangerous
					double dangerDistance=1000;
					for (Feature myF : fc.features()) {//for a direction go through every feature and find the one with the shortest distance
						Point stationPoint = (Point) myF.geometry();
						double stationLatitude = (stationPoint.coordinates().get(1));
						double stationLongitude = (stationPoint.coordinates().get(0));
						double currentdangerDistance = Math.sqrt((Math.pow((currentPosition.nextPosition(dir).latitude-stationLatitude), 2))+(Math.pow((currentPosition.nextPosition(dir).longitude-stationLongitude), 2)));
						if (currentdangerDistance<dangerDistance) {
							closestFeature=myF;
							dangerDistance=currentdangerDistance;
						}
					}
					String symbol = (closestFeature.getStringProperty("marker-symbol"));
					if (symbol.equals("lighthouse")) {//direction will bring drone in range of danger but will be closer to lighthouse so no coin or power loss
						danger=0;
					}
					if (symbol.equals("danger")) {//direction will cause drone to lose coins and power
						danger=1;
					}
				}
			}
		}
		return danger;
	}

	private void findDistancesFromStation() {//calculates each directions distance from the current goal
		for (Direction dir : Direction.values()) {
			Position maybeNextPosition = currentPosition.nextPosition(dir);
			Point stationPoint = (Point) closestLighthouse.geometry();
			double stationLatitude = (stationPoint.coordinates().get(1));
			double stationLongitude = (stationPoint.coordinates().get(0));
			double distanceToStation = Math.sqrt((Math.pow((maybeNextPosition.latitude-stationLatitude), 2))+(Math.pow((maybeNextPosition.longitude-stationLongitude), 2)));
			distancesFromStation.put(dir, distanceToStation);//each direction mapped to a distance
		}
	}

	private Feature checkIfStationInRange() {//Searches for closest station in range of the current position, can return null if no station in range.
		HashMap<Feature,Double> stationsInRange = new HashMap<Feature,Double>();
		for (Feature f :fc.features()) {//Makes list of features in range
			Geometry currentGeometry = (Geometry) f.geometry();
			Point currentPoint = (Point) currentGeometry;
			double currentDistance = Math.sqrt((Math.pow((currentPoint.latitude()-currentPosition.latitude), 2))+(Math.pow((currentPoint.longitude()-currentPosition.longitude), 2)));
			if (currentDistance<0.00025) {//the station feature is reachable from the nextPosition
				stationsInRange.put(f, currentDistance);
			}
		}
		double lowestStationDistance=1000;//At this point stationsInRange has all the stations in range of the current position.
		Feature closestStationInRange=null;
		for (Entry<Feature, Double> f: stationsInRange.entrySet()) {//finds the station closest in range
			if(f.getValue()<lowestStationDistance) {
				lowestStationDistance=f.getValue();
				closestStationInRange=f.getKey();
			}
		}
		return closestStationInRange;
	}
}
	