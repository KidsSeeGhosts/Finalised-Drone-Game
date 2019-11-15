package uk.ac.ed.inf.powergrab;



import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.Reader;
//import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
//import java.util.Arrays;
import java.util.Scanner;

//import com.mapbox.geojson.FeatureCollection;
//import com.mapbox.geojson.Feature;
//import com.mapbox.geojson.Geometry;
//import com.mapbox.geojson.Point;
//import com.mapbox.geojson.LineString;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonSyntaxException;


/**
 * Hello world!
 *
 */
public class App 
{
    private static Scanner scanner;

	public static void main( String[] args )
    {
    		
    		int userdate = Integer.parseInt(args[0]);//Parsing user input
    		int usermonth = Integer.parseInt(args[1]);
    		int useryear = Integer.parseInt(args[2]);
    		double userlatitude = Double.parseDouble(args[3]);
    		double userlongitude = Double.parseDouble(args[4]);
    		int userSeed = Integer.parseInt(args[5]);
    		String dronetype = args[6];
//        System.out.println(userdate);
//        System.out.println(usermonth);
//        System.out.println(useryear);
//        System.out.println(userlatitude);
//        System.out.println(userlongitude);
//        System.out.println(userSeed);
//        System.out.println(dronetype);
        
        
        //THIS SECTION OF THE CODE ALLOWS ME TO GET EVERY SINGLE URL POSSIBLE
//        String startofurl = "http://homepages.inf.ed.ac.uk/stg/powergrab/";
//        String endofurl = "/powergrabmap.geojson";
//        String[] years = new String [] {"2019","2020"};
//        String[] months = new String [] {"01","02","03","04","05","06","07","08","09","10","11","12"};
//        String[] monthsWith30days = new String [] {"09", "04", "06", "11"};
//        String[] monthsWith31days = {"01","03","05","07","08","10","12"};
//        for (String year : years) {
//        		for (String month : months) {
//        			if(Arrays.asList(monthsWith30days).contains(month)) {
//        				for (int i = 1 ; i<=30;i++) {
//        					if (i<10) {//need a 0 before date if less than 10
//        						String myurlstring = startofurl+year+"/"+month+"/0"+i+endofurl;
//        					}
//        					else {
//        						String myurlstring = startofurl+year+"/"+month+"/"+i+endofurl;
//        					}
//        				}
//        			}
//        			if (Arrays.asList(monthsWith31days).contains(month)) {
//        				for (int i = 1 ; i<=31;i++) {
//        					if (i<10) {//need a 0 before date if less than 10
//        						String myurlstring = startofurl+year+"/"+month+"/0"+i+endofurl;
//        					}
//        					else {
//        						String myurlstring = startofurl+year+"/"+month+"/"+i+endofurl;
//        					}
//        				}
//        			}
//        			else if (month.equals("02")){//it's february
//        				if (year.equals("2019")){//28 days
//            				for (int i = 1 ; i<=28;i++) {
//            					if (i<10) {//need a 0 before date if less than 10
//            						String myurlstring = startofurl+year+"/"+month+"/0"+i+endofurl;
//            					}
//            					else {
//            						String myurlstring = startofurl+year+"/"+month+"/"+i+endofurl;
//            					}
//            				}
//            			}
//        				if (year.equals("2020")){//29days
//            				for (int i = 1 ; i<=29;i++) {
//            					if (i<10) {//need a 0 before date if less than 10
//            						String myurlstring = startofurl+year+"/"+month+"/0"+i+endofurl;
//            					}
//            					else {
//            						String myurlstring = startofurl+year+"/"+month+"/"+i+endofurl;
//            					}
//            				}
//            			}
//        			}	
//        		}
//
//        }
        
        //THIS SECTION OF THE CODE ALLOWS ME TO PARSE THE GEOJSON AT A PARTICULAR URL
    		String usermonthString = Integer.toString(usermonth);
    		String userdateString = Integer.toString(userdate);
    		if (usermonth<10) {
    			usermonthString = ("0"+usermonth);
    		}
    		if (userdate<10) {
    			userdateString = ("0"+userdate);
    		}
        String mapString = "http://homepages.inf.ed.ac.uk/stg/powergrab/"+useryear+"/"+usermonthString+"/"+userdateString+"/powergrabmap.geojson";
        System.out.println(mapString);
        URL mapURL = null;
		try {
			mapURL = new URL(mapString);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) mapURL.openConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        try {
			conn.setRequestMethod("GET");
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        conn.setDoInput(true);
        try {
			conn.connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        InputStream myinputstream = null;
		try {
			myinputstream = conn.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scanner = new Scanner(myinputstream);
		Scanner s = scanner.useDelimiter("\\A");//converting input stream to string to it can be parsed
		String mapSource = s.hasNext() ? s.next() : "";
		
		//FINISHED PARSING GEOJSON. MAPSOURCE CONTAINS THE FULL GEOJSON FROM THE WEBSERVER AT THIS STAGE.
		
        Position startPosition = new Position(userlatitude,userlongitude);
        String nameuserdate = Integer.toString(userdate);
        String nameusermonth = Integer.toString(usermonth);
        if ((userdate)<10) {
        		nameuserdate="0"+nameuserdate;
        }
        if ((usermonth)<10) {
    		nameusermonth="0"+nameusermonth;
    }
        String newFileName = dronetype+"-"+nameuserdate+"-"+nameusermonth+"-"+Integer.toString(useryear)+".txt";
        		//System.out.println(newFileName);
        
        if (dronetype.equals("stateless")){
        		StatelessDrone statelessdrone = new StatelessDrone(startPosition,userSeed,mapSource,newFileName);//makes a stateless drone
        		statelessdrone.run();//the drone will now run through the given map
        }
        
        
        if (dronetype.equals("stateful")){
        		StatefulDrone statefuldrone = new StatefulDrone(startPosition,userSeed,mapSource,newFileName);//makes a stateful drone
        		statefuldrone.run();//the drone will now run through the given map
        }
        
    }
    
}