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
	
    
    
	

	public StatelessDrone(Position startposition, int userSeed) {
		// TODO Auto-generated constructor stub
		this.startposition=startposition;
		this.userSeed=userSeed;
		this.power=250;
	}




	public void run(String mapSource, String newFileName) {
		PrintWriter writer = null;
		String geojsonFileName = (newFileName.substring(0, newFileName.length() - 3))+"geojson";
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
		currentposition=startposition;
		int moves =0;//everytime i make move position i'll increment this
		
		while (power>0 && moves<=250) {//this is for each move
			HashMap<Direction,Feature> visitablePoints = new HashMap<Direction, Feature>();//each direction will have one feature (station)
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
			
			//at this stage the drone can't see any stations from its current position
	        if (visitablePoints.keySet().isEmpty()) {//time to pick randomly between the 16 directions and make the move
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
	        
			if(!(visitablePoints.keySet().isEmpty())) {//there's directions we can visit
				System.out.println(visitablePoints.keySet());
				Direction bestdirection = null;
				double highestCoins = -1000;
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
				//Right now, I pick the first direction that gives highest coins. I don't care if it's not the closest I get to the station because accuracy doesn't matter.
				System.out.println(bestdirection);
				System.out.println(highestCoins);
				if (highestCoins<=0) {//we should pick a random direction that is not one of the visitable points directions
					System.out.println("RANDOM MOVE BECAUSE HIGHEST COINS AVAILABLE IN CURRENT POSITION IS 0");
		        		int random_integer = rand.nextInt(15);
			        	Direction nextone = Direction.values()[random_integer];//0 is N ... and 15 is NNW
			        	while (!(currentposition.nextPosition(nextone).inPlayArea())||(visitablePoints.keySet().contains(nextone))) {//if it's next position not in play area and direction is not a bad direction with 0 or less coins
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
				//At this point I know where I want to move and the consequence
				else {
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
			}
			
			
			
//	        for (Feature f : fc.features()) {//checking each station to see if it's visitable from the currentposition
//	        		Geometry currentgeometry = (Geometry) f.geometry();
//	        		Point currentpoint = (Point) currentgeometry;
//	        		for (Direction dir : Direction.values()) {//checking if the station can be reached from any of the potential 16 moves
//	        			Position maybenext = currentposition.nextPosition(dir);
//	        			double distancebetween = Math.sqrt((Math.pow((currentpoint.latitude()-maybenext.latitude), 2))+(Math.pow((currentpoint.longitude()-maybenext.longitude), 2)));
//	        			if (distancebetween<0.00025) {//visitable point
//	        		
//		        				if (visitablePoints.containsKey(dir)) {//if the station can be already be reached from an alternate position
//		        					if (f.getProperty("coins").getAsDouble()>visitablePoints.get(dir).getProperty("coins").getAsDouble()) {//if new property has greater coins than the current one
//		        						
//		        					}
//		        					//visitablePoints.put(dir,f);
//		        					//Position oldmaybenext = visitablePoints.get(currentpoint);
//		        					//double olddistancebetween = Math.sqrt((Math.pow((currentpoint.latitude()-oldmaybenext.latitude), 2))+(Math.pow((currentpoint.longitude()-oldmaybenext.longitude), 2)));
//		        					//if (distancebetween>olddistancebetween) {
//		        						//nextpositions.put(maybenext, dir);
//		        					//}
//		        					
//		        				}
//		        				else {//direction has not been gone across before
//		        					ArrayList<Feature> features = new ArrayList<Feature>();
//		        					features.add(f);
//		        					visitablePoints.put(dir, features);
//		        					//nextpositions.put(maybenext, dir);
//		        				}
//	        			
//	        			}
//	        		}
//	        }
	        
	       
//	        
//	        else if (!(visitablePoints.isEmpty() || visitablePoints==null)){//visitable points is not empty, time to decide where to move after seeing our options
//	        System.out.println("NON RANDOM VISITABLE POINTS");
//	        	double propertycoins=0;
//	        	Position futureposition = null;
//	        	double propertypower = 0;
//	        	double positionCoins = 0;
//	        	double positionPower = 0;
//	        	double[] allPositionValues = new double[16];
//	        	double[] allPositionPower = new double[16];
//	        	int i = 0;
//	        	for (Direction dir : Direction.values()) {
//	        		positionCoins = 0;
//	        		positionPower = 0;
//	        		if ((visitablePoints.get(dir)!=null)) {
//	        		for (Feature f : visitablePoints.get(dir)){//deciding which of the visitable sations has the most coins
//	        				propertycoins = f.getProperty("coins").getAsDouble();
//	        				propertypower=f.getProperty("power").getAsDouble();
//	        				positionCoins = positionCoins + propertycoins;
//	        				positionPower = positionPower + propertypower;
//	        		}
//	        		}
//	        		allPositionValues[i] = positionCoins;
//	        		allPositionPower[i] = positionPower;
//	        		i++;
	        		//now we have the total coins you'd get going to that position
	        		
	        		
//	        	}
	        //	System.out.println(Arrays.toString(allPositionValues));
//			double max = 0;
//			int index = 0;
//			for(int j = 0; j < allPositionValues.length; j ++){
//			    if(max < allPositionValues[j]){
//			        max = allPositionValues[j];
//			        index = j;
//			    }
//			}
//			//System.out.println(max);//finding index(direction) with most coins
//			int k=0;
//			ArrayList<Integer> myindex = new ArrayList<Integer>();//this is so that if say north and north west have same value of coins, we'll randomly choose one
//			 while (k < 16) { 
//		            if (allPositionValues[k] == max) { 
//		                myindex.add(k);
//		                //System.out.println(k);
//		                k++;
//		            }
//		            else {
//		            	k++;
//		            }
//		        } 
			 //System.out.println(Arrays.toString(myindex.toArray()));
//			int bestIndex = rand.nextInt(myindex.size());
//			//System.out.println(myindex.get(bestIndex));
//			Direction finaldirection = Direction.values()[myindex.get(bestIndex)];
//			coins=coins+allPositionValues[bestIndex];
//			power=power+allPositionPower[bestIndex];
//			Position bestposition = currentposition.nextPosition(finaldirection);
//			writer.println(currentposition.latitude+","+currentposition.longitude+","+finaldirection+","+bestposition.latitude+","+bestposition.longitude+","+coins+","+power);
//			currentposition = bestposition;
//			coins=coins+allPositionValues[bestIndex];
//			power=power+allPositionPower[bestIndex];
//			power=power-1.25;
//			moves++;
			//time to decrease the charing station
			//System.out.println("VISITED PROPERTIES");
			//System.out.println(visitablePoints.get(finaldirection));
//			if (visitablePoints.get(finaldirection)!=null) {
//				for (Feature f : visitablePoints.get(finaldirection)){//subtract coins and power from each station visited
//					f.removeProperty("coins");
//					f.addStringProperty("coins", "0.0");
//					f.removeProperty("power");
//					f.addStringProperty("power", "0.0");
//				}
//			}
			//System.out.println("AFTER MAKING COINS AND POWER 0");
			//System.out.println(visitablePoints.get(finaldirection));
			
			
	        		//power = power+propertypower;
	        		//coins=coins+propertycoins;
	        		//power=power-1.25;
	        		//Direction nextone = nextpositions.get(futureposition);
	        		//System.out.println(currentposition.latitude+","+currentposition.longitude+","+nextone+","+futureposition.latitude+","+futureposition.longitude+","+coins+","+(power-1.25));
	        		//writer.println(currentposition.latitude+","+currentposition.longitude+","+nextone+","+futureposition.latitude+","+futureposition.longitude+","+coins+","+(power-1.25));
	        		//currentposition = futureposition;
	        		//moves++;
//	        }
	        //moves++;
	     
	     
	        //after going through each station we will have a list of visitable points(stations) and the position you will move to if you choose that point.
//	        System.out.println("coins");
//	        System.out.println(coins);
//	        System.out.println("power");
//	        System.out.println(power);
//	        System.out.println("moves");
//			System.out.println(moves);
		}
		writer.close();
		//at this point the stateless drone's journey is complete
		System.out.println(currentposition.latitude);
		System.out.println(currentposition.longitude);
		System.out.println(power);
		System.out.println(coins);
		System.out.println(moves);
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
//		System.out.println(power);
//		System.out.println(moves);
	
	}

}
