package uk.ac.ed.inf.powergrab;



import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        //Testing nextPosition
        //Position start = new Position(55.944425,-3.188396);
        ///System.out.println(start.nextPosition(Direction.SSE).latitude);
        //System.out.println(start.nextPosition(Direction.SSE).longitude);
        //System.out.println((0.0003*Math.sin(67.5)));
        //System.out.println(Math.sin(67.5));
        //System.out.println(0.0003*Math.sin(Math.toRadians(67.5)));//h2
        //System.out.println(0.0003*Math.cos(Math.toRadians(67.5)));//w2
        //	System.out.println(0.0003*Math.sin(Math.toRadians(45))); //h3
        //	System.out.println(0.0003*Math.cos(Math.toRadians(45))); //w3
        	//System.out.println(0.0003*Math.sin(Math.toRadians(22.5))); //h4
        //	System.out.println(0.0003*Math.cos(Math.toRadians(22.5))); //w4
        String mapString = "http://homepages.inf.ed.ac.uk/stg/powergrab/2019/01/01/powergrabmap.geojson";
        URL mapUrl = null;//the try catch makes sure an exception isn't thrown at runtime
		try {
			mapUrl = new URL(mapString);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//throws exception when we don't have a well formed url
        HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) mapUrl.openConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//downcasting url connection to a http url connection
        InputStream myinputstream = null;
		conn.setReadTimeout(10000);
		conn.setConnectTimeout(15000);
		try {
			conn.setRequestMethod("GET");
		} catch (ProtocolException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		conn.setDoInput(true);
		try {
			conn.connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			myinputstream = conn.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        String mapSource = myinputstream.toString();
        
    }
    
}