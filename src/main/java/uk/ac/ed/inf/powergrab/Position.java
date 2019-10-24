package uk.ac.ed.inf.powergrab;


public class Position {


    public double latitude;
    public double longitude;
    
    //My trig length results are static final constants, therefore the lengths are only calculated once and don't need to be recalculated...
    //...each time nextPosition is called or a new position class is created in order to make the program more efficient.
    //I am also able to calculate all next positions using only 3 lengths rather than 6 since cosine and sine equal opposite at certain degrees e.g height r(sin67.5) == width r(cos22.5).
    //O.0003 is used as the hypotenuse length r as it is the distance the drone moves in one move.
	private static final double h2 = 0.0003*Math.sin(Math.toRadians(67.5));//r*sin67.5 length
	private static final double h3 = 0.0003*Math.sin(Math.toRadians(45)); //r*sin45 length
	private static final double h4 = 0.0003*Math.sin(Math.toRadians(22.5)); //r*sin22.5 length
    
	
    public Position (double latitude, double longitude) {//Defining position type for longitude,latitude coordinates.
    		this.latitude = latitude;
    		this.longitude = longitude;
    		
    }

    //Going North increases latitude.
    //Going South decreases latitude.
    //Going East increases longitude.
    //Going West decreases.
    public Position nextPosition (Direction direction) {//This function returns the next position of the drone when it makes a move in the specified compass direction.
    		if (direction == Direction.N) {
//    			return new Position(latitude+0.0003,longitude);//0.0003 is how far the drone moves in one move.
    		}
    		if (direction == Direction.NNE) { //Adding to longitude and latitude as we go East.
    			return new Position(latitude+h2,longitude+h4);
    		}
    		if (direction == Direction.NE) {
    			return new Position(latitude+h3,longitude+h3);
    		}
    		if (direction == Direction.ENE) {
    			return new Position(latitude+h4,longitude+h2);
    		}
    		if (direction == Direction.E) {
    			return new Position(latitude,longitude+0.0003);
    		}
    		if (direction == Direction.ESE) {//Still adding to longitude, subtracting from latitude as we go South.
    			return new Position(latitude-h4,longitude+h2);
    		}
    		if (direction == Direction.SE) {
    			return new Position(latitude-h3,longitude+h3);
    		}
    		if (direction == Direction.SSE) {
    			return new Position(latitude-h2,longitude+h4);
    		}
    		if (direction == Direction.S) {
    			return new Position(latitude-0.0003,longitude);
    		}
    		if (direction == Direction.SSW) {//Now decreasing the longitude and latitude as we go West.
    			return new Position(latitude-h2,longitude-h4);
    		}
    		if (direction == Direction.SW) {
    			return new Position(latitude-h3,longitude-h3);
    		}
    		if (direction == Direction.WSW) {
    			return new Position(latitude-h4,longitude-h2);
    		}
    		if (direction == Direction.W) {
    			return new Position(latitude,longitude-0.0003);
    		}
    		if (direction == Direction.WNW) {//Starting to increase latitude again and continue decreasing longitude as we return to North.
    			return new Position(latitude+h4,longitude-h2);
    		}
    		if (direction == Direction.NW) {
    			return new Position(latitude+h3,longitude-h3);
    		}
    		if (direction == Direction.NNW) {
    			return new Position(latitude+h2,longitude-h4);
    		}
    		else {//This else should never be reached as all valid direction types are covered.
    			return new Position(latitude,longitude);
    		}
    }
    
    //The inPlayArea function returns true if the position is in the play area defined by 2 latitudes and 2 longitudes.
    //The position latitude must be between 55.942617 and 55.946233.
    //The position longitude must be between -3.192473 and -3.184319.
    //The function returns false if the drone's position is not within the play area or on the edge of the play area.
    public boolean inPlayArea() {
    		if (latitude<55.946233 && latitude>55.942617 && longitude<-3.184319 && longitude>-3.192473){
    			return true;
    		}
    		else {
    			return false;
    		}
    }
	
	
}