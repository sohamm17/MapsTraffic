
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.TravelMode;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import javax.json.*;
import javax.json.stream.JsonParser;

public class MapsTrafficMain {

	static final String Google_PUBLIC_API_KEY = "AIzaSyBNJndqJCWHbRrZ4DbGzWEQ19ROJYfM8wg";
	static Random rand;

	public static void main(String[] args) throws Exception {
		
		MapsTrafficMain myMap = new MapsTrafficMain();
		//myMap.hereMapsAPI();
		
		Routing srcToDstRoute = new Routing();
		double walkingSpeed = 0.5; //TODO Have to get from GoogleFit
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		@SuppressWarnings("deprecation")
		Date date = new Date();//new Date(115, 02, 26, 17, 30, 00);
		System.out.println(dateFormat.format(date));
		String source_string = "7708 109 Street Edmonton NW, AB, Canada";
		String destination_string = "University of Alberta, AB, Canada";
		
		//Geo-coding of heremaps is not very good
		srcToDstRoute.getRoute(source_string, destination_string, walkingSpeed, dateFormat.format(date));
		
		Traffic realTimeTraffic = new Traffic();
		
		RouteInfo.maneuvar.latlon src_latlon, dst_latlon;
		
		src_latlon = Routing.getLatLong(source_string);
		dst_latlon = Routing.getLatLong(destination_string);
		
		String[] crossingRoads = {"82 Av", "78 Av"};
		System.out.println(
				realTimeTraffic.getAverageSpeed(src_latlon.lat, src_latlon.lon, dst_latlon.lat, 
						dst_latlon.lon, "109 St", "+", crossingRoads)
				);
		
		System.out.println(
				realTimeTraffic.getAverageSpeed(src_latlon.lat, src_latlon.lon, dst_latlon.lat, 
						dst_latlon.lon, "82 Av", "+", new String[] {"111 St", "112 St", "109 St", "114 St"})
				);
		
		System.out.println(
				realTimeTraffic.getAverageSpeed(src_latlon.lat, src_latlon.lon, dst_latlon.lat, 
						dst_latlon.lon, "114 St", "+", new String[] {"82 Av", "83 Av", "84 Av", "85 Av", "86 Av", "87 Av"})
				);
		
		/*
		rand = new Random(50);// giving seed value to get finite result everytime
		GeoApiContext context = new GeoApiContext().setApiKey(Google_PUBLIC_API_KEY);
		
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
					+ "\n" + eachStep.duration.inSeconds + "sec - "
					+ eachStep.travelMode.toString() + " - " + ((eachStep.subSteps != null) ? eachStep.subSteps.length : 0) + " - "
					+ eachStep.htmlInstructions
					+ " - " + eachStep.distance.inMeters + "m\n");
//			System.out.println("Modified");
//			addDelay(eachStep);
//			System.out.println(eachStep.duration.inSeconds + "sec - "
//					+ eachStep.travelMode.toString() + " - " + ((eachStep.subSteps != null) ? eachStep.subSteps.length : 0) + " - "
//					+ eachStep.htmlInstructions
//					+ " - " + eachStep.distance.inMeters + "m\n");
		}
		System.out.println("Starting time: " + routes[0].legs[0].arrivalTime.toLocalDateTime());
		*/
	}
	
	//adding delay to particular step.
	public static void addDelay(DirectionsStep step)
	{
		int delay = rand.nextInt(10);
		System.out.println("random delay:" + delay * 60);
		step.duration.inSeconds += delay *60;
	}
}