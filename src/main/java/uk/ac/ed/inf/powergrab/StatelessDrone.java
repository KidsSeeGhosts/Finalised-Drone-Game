package uk.ac.ed.inf.powergrab;

import java.util.HashMap;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class StatelessDrone extends Drone{
	
	private double highestCoins;
	private Direction bestdirection;
	private HashMap<Direction,Feature> visitablePoints;
	
	public StatelessDrone(Position startposition, int userSeed, String mapSource,String newFileName) {
		this.startposition=startposition;
		this.userSeed=userSeed;
		this.power=250;
		this.mapSource=mapSource;
		this.newFileName=newFileName;
	}

	public void run() {
		setUpDrone();
		while (power>0 && moves<=250) {//this is for each move
			visitablePoints = new HashMap<Direction, Feature>();//each direction will have one feature (station)
			searchForStations();
	        if (visitablePoints.keySet().isEmpty()) {//at this stage the drone can't see any stations from its current position
	        		makeRandomMove();//time to pick randomly between the 16 directions and make the move
	        }
			if(!(visitablePoints.keySet().isEmpty())) {//there's stations we can visit
				highestCoins = -1000;
				bestdirection = findBestDirection();
				if (highestCoins<=0) {//we should pick a random direction that is not one of the visitable points directions
					makeNoCoinGainMove();
				}
				else if (highestCoins>0){//At this point I know where I want to move and the consequence is positive
					makeCoinGainMove();
				}
			}
		}
		writer.close();//at this point the stateless drone's journey is complete
		makeNewGeoJson();
	}

	private void makeNoCoinGainMove() {//Visitable points take away any feature with negative coin!
		for (Direction d : Direction.values()) {//Here I take lighthouses out of visitable points so that a lighthouse can be visited even if it has 0 coins
			Feature f = visitablePoints.get(d);
			if (f!=null) {
				String symbol = (f.getStringProperty("marker-symbol"));
				if (symbol.equals("lighthouse")) {
					visitablePoints.remove(d);
				}
			}
		}	
    		int random_integer = rand.nextInt(15);
        	Direction nextone = Direction.values()[random_integer];//0 is N ... and 15 is NNW
        	while ( (!(currentposition.nextPosition(nextone).inPlayArea())) || (visitablePoints.keySet().contains(nextone))  ) {//if it's next position not in play area and direction is not a bad direction with 0 or less coins
        		random_integer = rand.nextInt(15);
	        	nextone = Direction.values()[random_integer];
        	}
        moveDrone(nextone);
	}

	private void makeCoinGainMove() {
		coins=coins+visitablePoints.get(bestdirection).getProperty("coins").getAsDouble();
		power=power+visitablePoints.get(bestdirection).getProperty("power").getAsDouble();
		moveDrone(bestdirection);
		Feature featureVisited = visitablePoints.get(bestdirection);
		drainStation(featureVisited);
	}

	private void searchForStations() {
		for (Direction dir : Direction.values()) {
			Position maybeNextPosition = currentposition.nextPosition(dir);
			double lowestDistance = -1;
			for (Feature f : fc.features()) {//for a direction go through every feature and find the one with the shortest distance
				Geometry currentgeometry = (Geometry) f.geometry();
        			Point currentpoint = (Point) currentgeometry;
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
	
	private void makeRandomMove() {//for when there are no stations in vicinity
		int random_integer = rand.nextInt(15);
	    	Direction nextone = Direction.values()[random_integer];//0 is N ... and 15 is NNW
	    	while (!(currentposition.nextPosition(nextone).inPlayArea())) {//if its next position not in play area
	    		random_integer = rand.nextInt(15);
	        	nextone = Direction.values()[random_integer];
	    	}
	    	moveDrone(nextone);
	}
	
	private Direction findBestDirection() {//Need to change this later on as it should pick randomly between the directions with same number of coins and right now it just does the first.
		for (Direction d : visitablePoints.keySet()) {//find the directions that give max number of coins
			Feature f = visitablePoints.get(d);
			double stationCoins = f.getProperty("coins").getAsDouble();
			if (highestCoins==-1000) {//if it's the first station set initialise its coins to be the highest
				highestCoins=stationCoins;
				bestdirection=d;
			}
			else {
				if (stationCoins>highestCoins) {
					highestCoins=stationCoins;
					bestdirection=d;
				}
			}
		}
		return bestdirection;
	}
}
