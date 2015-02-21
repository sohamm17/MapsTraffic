
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.TravelMode;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/** Java "Hello, world!" example using the client library */
public class HelloWorld {

	static final String PUBLIC_API_KEY = "AIzaSyBNJndqJCWHbRrZ4DbGzWEQ19ROJYfM8wg";

	public static void main(String[] args) throws Exception {

		// Replace the API key below with a valid API key.
		GeoApiContext context = new GeoApiContext().setApiKey(PUBLIC_API_KEY);
//		GeocodingResult[] results =  GeocodingApi.geocode(context,
//		    "7708 109 Street Edmonton").await();
//		System.out.println(results[0].formattedAddress);
		
		DirectionsRoute[] routes = DirectionsApi.newRequest(context)
		        .origin("7708 109 Street Edmonton NW, AB, Canada")
		        .destination("Athabasca Hall, University of Alberta, AB, Canada")
		        .departureTime(new DateTime(2015, 2, 19, 9, 0, DateTimeZone.getDefault()))
		        .mode(TravelMode.TRANSIT)
		        .await();
		
		System.out.println("Printing the first route: " + routes[0].toString());
		System.out.println("Starting time: " + routes[0].legs[0].departureTime.toLocalDateTime());
		for(DirectionsStep eachStep: routes[0].legs[0].steps)
		{
			System.out.println(eachStep.startLocation + "\tTo\t" + eachStep.endLocation 
					+ "\n" + eachStep.duration.humanReadable + " - "
					+ eachStep.travelMode.toString() + " - " + ((eachStep.subSteps != null) ? eachStep.subSteps.length : 0) + " - "
					+ eachStep.htmlInstructions
					+ " - " + eachStep.distance.inMeters + "m\n");
		}
		System.out.println("Starting time: " + routes[0].legs[0].arrivalTime.toLocalDateTime());
	}
}