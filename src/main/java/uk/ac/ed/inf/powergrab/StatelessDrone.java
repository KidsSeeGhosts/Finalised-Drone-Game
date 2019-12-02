package uk.ac.ed.inf.powergrab;

import java.math.BigDecimal;
import java.util.HashMap;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class StatelessDrone extends Drone{

	private double highestCoins;
	private Direction bestDirection;
	private HashMap<Direction,Feature> visitablePoints;//maps each direction's next position to its nearest station

	public StatelessDrone(Position startposition, int userSeed, String mapSource,String newFileName) {
		this.startPosition=startposition;
		this.userSeed=userSeed;
		this.power=new BigDecimal(250);
		this.mapSource=mapSource;
		this.newFileName=newFileName;
	}

	public void run() {//starts the drone's gameplay
		setUpDrone();
		while (power.doubleValue()>0 && moves<250) {//Start of a move
			visitablePoints = new HashMap<Direction, Feature>();//visitable points resets each move as distances from stations change
			searchForStations();
			if (visitablePoints.keySet().isEmpty()) {//at this stage the drone can't see any stations from its current position
				makeRandomMove();//time to pick randomly between the 16 directions and make the move
			}
			if(!(visitablePoints.keySet().isEmpty())) {//there are stations we can visit
				highestCoins = -1000;
				bestDirection = findBestDirection();
				if (highestCoins<=0) {//we should pick a random direction that is not one of the visitable points directions
					makeNoCoinGainMove();
				}
				else if (highestCoins>0){//At this point the drone knows where it wants to move and the consequence is positive
					makeCoinGainMove();
				}
			}
			writer.println(coins.doubleValue()+","+power.doubleValue());
		}
		writer.close();//at this point the stateless drone's journey is complete so the moves and geojson files are now produced
		makeNewGeoJson();
	}

	private void makeNoCoinGainMove() {
		for (Direction d : Direction.values()) {//Here I take lighthouses out of visitable points so that a lighthouse can be revisited even if it has 0 coins
			Feature f = visitablePoints.get(d);
			if (f!=null) {
				String symbol = (f.getStringProperty("marker-symbol"));
				if (symbol.equals("lighthouse")) {
					visitablePoints.remove(d);
				}
			}
		}//At this stage, visitable points only contains bad directions to be avoided
		int random_integer = rand.nextInt(15);
		Direction nextone = Direction.values()[random_integer];//0 is N ... and 15 is NNW
		while ((!(currentPosition.nextPosition(nextone).inPlayArea())) || (visitablePoints.keySet().contains(nextone))  ) {//checks if next position not in play area or is a bad position with 0 or less coins
			random_integer = rand.nextInt(15);
			nextone = Direction.values()[random_integer];
		}
		moveDrone(nextone);
	}

	private void makeCoinGainMove() {//moves drone, changes drone and station's values
		moveDrone(bestDirection);
		Feature featureVisited = visitablePoints.get(bestDirection);
		BigDecimal stationCoins = new BigDecimal(featureVisited.getProperty("coins").getAsDouble());
		BigDecimal stationPower = new BigDecimal(featureVisited.getProperty("power").getAsDouble());
		coins=coins.add(stationCoins);
		power=power.add(stationPower);
		drainStation(featureVisited);
	}

	private void searchForStations() {//maps each direction to its closest station in range
		for (Direction dir : Direction.values()) {
			Position maybeNextPosition = currentPosition.nextPosition(dir);
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
		while (!(currentPosition.nextPosition(nextone).inPlayArea())) {//if its next position not in play area, randomly select different direction
			random_integer = rand.nextInt(15);
			nextone = Direction.values()[random_integer];
		}
		moveDrone(nextone);
	}

	private Direction findBestDirection() {//find the directions that give max number of coins
		for (Direction d : visitablePoints.keySet()) {
			Feature f = visitablePoints.get(d);
			double stationCoins = f.getProperty("coins").getAsDouble();
			if (highestCoins==-1000 && currentPosition.nextPosition(d).inPlayArea()) {//if it's the first station set initialise its coins to be the highest
				highestCoins=stationCoins;
				bestDirection=d;
			}
			else {
				if (stationCoins>highestCoins && currentPosition.nextPosition(d).inPlayArea()) {
					highestCoins=stationCoins;
					bestDirection=d;
				}
			}
		}
		return bestDirection;
	}
}
