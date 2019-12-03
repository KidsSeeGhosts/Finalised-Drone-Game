package uk.ac.ed.inf.powergrab;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

public class App 
{
	public static void main(String[] args){
    		int userdate = Integer.parseInt(args[0]);//Parsing user input
    		int usermonth = Integer.parseInt(args[1]);
    		if (!(usermonth<=12 && usermonth>=1)) {//check if user selected valid month
    			throw new IllegalArgumentException("Month must be between 1 and 12");
    		}
    		int useryear = Integer.parseInt(args[2]);
    		if (!(useryear==2020) && !(useryear==2019)) {//check if user selected valid year
    			throw new IllegalArgumentException("Year must be 2019 or 2020.");
    		}
    		//checks if date is valid in 2019 and 2020.
    		if ((userdate==29 && usermonth==2 && useryear==2019) || (userdate>=30 && usermonth==2) ||((userdate==31) && (usermonth==4 || usermonth==6 || usermonth==9 || usermonth==11)) || (userdate>31)|| (userdate<1)) {
    			throw new IllegalArgumentException("Invalid date for 2019/2020");
    		}
    		double userlatitude = Double.parseDouble(args[3]);
    		double userlongitude = Double.parseDouble(args[4]);
    		try{//checks if seed is an integer and throws exception if it is not
    			Integer.parseInt(args[5]);
    		} catch(NumberFormatException e){
    			throw new IllegalArgumentException("Seed is not of integer type.");
    		}
    		int userSeed = Integer.parseInt(args[5]);
    		String dronetype = args[6];
    		String usermonthString = Integer.toString(usermonth);
    		String userdateString = Integer.toString(userdate);
    		if (usermonth<10) {//allows user to input single digits months and dates as single digit instead of double digit e.g 5 and 05 is fine.
    			usermonthString = ("0"+usermonth);
    		}
    		if (userdate<10) {
    			userdateString = ("0"+userdate);
    		}
    		Position startPosition = new Position(userlatitude,userlongitude);
    		if (!(startPosition.inPlayArea())) {//checks if starting position is valid
    			throw new IllegalArgumentException("Start position inputed out of play area bounds.");
    		}
    		if (!(dronetype.equals("stateful")) && !(dronetype.equals("stateless"))) {//check if user selected mode is valid
    			throw new IllegalArgumentException("Invalid mode inputed. Must be stateless or stateful.");
    		}
    		
    		//Parsing GeoJSON at a particular URL.
        String mapString = "http://homepages.inf.ed.ac.uk/stg/powergrab/"+useryear+"/"+usermonthString+"/"+userdateString+"/powergrabmap.geojson";
        System.out.println(mapString);
        URL mapURL = null;
		try {
			mapURL = new URL(mapString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
        HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) mapURL.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        try {
			conn.setRequestMethod("GET");
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
        conn.setDoInput(true);
        try {
			conn.connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
        InputStream myinputstream = null;
		try {
			myinputstream = conn.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Scanner scanner = new Scanner(myinputstream);
		Scanner s = scanner.useDelimiter("\\A");//converting input stream to string to it can be parsed
		String mapSource = s.hasNext() ? s.next() : "";
		scanner.close();
		//mapSource contains the full GeoJSON file from the server. Parsing of GeoJSON complete.
		
        String newFileName = dronetype+"-"+userdateString+"-"+usermonthString+"-"+Integer.toString(useryear)+".txt";
        FeatureCollection fc =FeatureCollection.fromJson(mapSource);//Finding total coins in the current map to calculate optimality
        BigDecimal totalCoins = new BigDecimal(0);
        for (Feature f : fc.features()) {
        		String symbol = (f.getStringProperty("marker-symbol"));
			if (symbol.equals("lighthouse")) {
        			BigDecimal stationCoins = new BigDecimal(f.getProperty("coins").getAsDouble());
        			totalCoins=totalCoins.add(stationCoins);
			}
        }
        
        if (dronetype.equals("stateless")){
        		StatelessDrone statelessdrone = new StatelessDrone(startPosition,userSeed,mapSource,newFileName);//makes a stateless drone
        		statelessdrone.run();//the drone will now run through the given map
        		System.out.println("Optimality: "+(statelessdrone.coins.divide(totalCoins, 2, RoundingMode.HALF_DOWN)).multiply(new BigDecimal(100)));//optimality of drone calculated and printed
        }
        
        
        if (dronetype.equals("stateful")){
        		StatefulDrone statefuldrone = new StatefulDrone(startPosition,userSeed,mapSource,newFileName);//makes a stateful drone
        		statefuldrone.run();
        		System.out.println("Optimality: "+(statefuldrone.coins.divide(totalCoins, 2, RoundingMode.HALF_DOWN)).multiply(new BigDecimal(100)));
        }
        
    }
    
}