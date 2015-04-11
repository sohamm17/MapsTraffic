
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
import java.util.ArrayList;
import java.util.Arrays;
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

@SuppressWarnings("unused")
public class MapsTrafficMain {

	//static final String Google_PUBLIC_API_KEY = "AIzaSyBNJndqJCWHbRrZ4DbGzWEQ19ROJYfM8wg";
	static Random rand;

	public static void main(String[] args) throws Exception {
		
		//MapsTrafficMain myMap = new MapsTrafficMain();
		//myMap.hereMapsAPI();
		
		GoogleOauth gO = new GoogleOauth();
		//String token = gO.retrieve("");
		//System.out.println("Printing the access token: " + token);
		
		Routing srcToDstRoute = new Routing();
		double walkingSpeed = 1.5;//gO.GoogleFitApi("");
		System.out.println("\nPrinting walking speed: " + walkingSpeed);
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		@SuppressWarnings("deprecation")
		Date date = new Date(115, 02, 30, 10, 47, 0);
		System.out.println(dateFormat.format(date));
		String source_string = "7708 109 Street, Edmonton, Alberta, Canada";
		String destination_string = "University of Alberta, AB, Canada";
		
		long rawRouteTime = System.nanoTime() / 1000000;
		RouteInfo route = srcToDstRoute.getRoute(source_string, destination_string, walkingSpeed, dateFormat.format(date));
		rawRouteTime = (System.nanoTime() / 1000000) - rawRouteTime;
		
		System.out.println("\n" + route);
		
		System.out.println("Route Calculation Time: " + rawRouteTime + "ms");
		
		
		RouteInfo.maneuvar.latlon src_latlon, dst_latlon;
		
		src_latlon = Routing.getLatLong(source_string);
		dst_latlon = Routing.getLatLong(destination_string);
		System.out.println(src_latlon);
		System.out.println(dst_latlon);
		
		Traffic realTimeTraffic = new Traffic(src_latlon, dst_latlon);
		
		//Real-time speed
		for(double ratio = 3; ratio >= 3; ratio -= .25)
		{
			route = srcToDstRoute.getRoute(source_string, destination_string, walkingSpeed, dateFormat.format(date));
			double trafficModuleAvgSpdFF = realTimeTraffic.getFreeFlowSpeed("82 Av", "+", 
					new String[] {"111 St", "112 St", "109 St", "114 St"});
			double trafficModuleAvgSpdSU = trafficModuleAvgSpdFF / ratio;
	//		realTimeTraffic.getAverageSpeedInTraffic("82 Av", "+", 
	//		new String[] {"111 St", "112 St", "109 St", "114 St"});
			
//			System.out.println();
//			realTimeTraffic.printCrossRoadInfo("82 Av", "+", 
//					new String[] {"111 St", "112 St", "109 St", "114 St"});
//			System.out.println();
			
			int modifyingIndex = 2;
			
			double avgSpeedInFFTotal = ((double)route.maneuvars.get(modifyingIndex).length / route.maneuvars.get(modifyingIndex).travelTime);
			double avgSpeedInTraffic =  trafficModuleAvgSpdSU * (avgSpeedInFFTotal / trafficModuleAvgSpdFF);   
			
	//		long trafficLength = route.maneuvars.get(modifyingIndex).interRoadAndStops.get(2).length 
	//				+ route.maneuvars.get(modifyingIndex).interRoadAndStops.get(3).length
	//				+ route.maneuvars.get(modifyingIndex).interRoadAndStops.get(4).length;
	//		long timeInTraffic = (long) (trafficLength / avgSpeedInTraffic);
	//		
	//		long timeWOTraffic =  (long) ((route.maneuvars.get(modifyingIndex).length - trafficLength) / avgSpeedInFFTotal);
	//		
	//		long totalTimeModified = route.maneuvars.get(modifyingIndex).travelTime;
	//		if(avgSpeedInTraffic < avgSpeedInFFTotal)
	//			totalTimeModified = (timeInTraffic + timeWOTraffic);
	//		
	//		
	//		System.out.println("Route FF Speed: " + avgSpeedInFFTotal + ", In Traffic Speed:" + avgSpeedInTraffic  + ", In Traffic length: " + trafficLength + "m");
	//		System.out.println("Bus riding time considering: " + totalTimeModified + "\twithout traffic:" + route.maneuvars.get(modifyingIndex).travelTime + "\n");
			
			long overhead = System.nanoTime() / 1000000;
			long trafficLength = 0;
			for(RouteInfo.maneuvar eachMan : route.maneuvars)
			{
				if(eachMan.type.compareTo("PublicTransportManeuverType") == 0)
				{
					String source_bus_stop = eachMan.stopName;
					double modifiedDuration = 0;
					double avgFFSpeed = ((double)eachMan.length / eachMan.travelTime);
					int i = 0;
					String commonRoad = null;
					for(RouteInfo.maneuvar.link eachLink : eachMan.interRoadAndStops)
					{
						String interStop = eachLink.nextStopName;
						double avgRealSpeed = Double.POSITIVE_INFINITY;
						if(getCommonRoad(source_bus_stop, interStop) != null)
							commonRoad = getCommonRoad(source_bus_stop, interStop);
						//System.out.print(commonRoad);
						//System.out.println(Arrays.toString(getUncommonRoads(source_bus_stop, interStop, commonRoad)));
						realTimeTraffic.getAverageSpeedInTraffic(commonRoad, "+", 
								getUncommonRoads(interStop, source_bus_stop, commonRoad));
						//realTimeTraffic.getFreeFlowSpeed("82 Av", "+", new String[] {"111 St", "112 St", "109 St", "114 St"});
						if(i == 2 || i == 3 || i == 4)
						{
							//avgRealSpeed = trafficModuleAvgSpdSU * (avgFFSpeed / trafficModuleAvgSpdFF);
							avgRealSpeed = avgFFSpeed / ratio; //traffic ratio is 2
						}
						//System.out.println(modifiedDuration);
						if(avgRealSpeed < avgFFSpeed && (i == 2 || i == 3 || i == 4))
						{
							modifiedDuration += ((double)eachLink.length / avgRealSpeed);
							trafficLength += eachLink.length;
						}
						else
							modifiedDuration += ((double)eachLink.length / avgFFSpeed);
						i++;
						//trafficLength += eachLink.length;
						source_bus_stop = interStop;
					}
					//System.out.println("Modified Duration: " + Math.round(modifiedDuration));
					route.modifyManeuvarTime(eachMan, Math.round(modifiedDuration));
				}
			}
			overhead = System.nanoTime() / 1000000 - overhead;
			//ratio = (avgSpeedInFFTotal / avgSpeedInTraffic);
			System.out.println(ratio + "\t" + route.getReachingTime() + "\t");
		}
		//route.modifyManeuvarTime(modifyingIndex, totalTimeModified);
		
		//System.out.println(route);
		
	}
	
	public static String getCommonRoad(String road1, String road2)
	{
		String[] partsRoad1 = road1.split("[+]");
		String[] partsRoad2 = road2.split("[+]");
		int street1 = Traffic.getAvenueOrStreetNumber(partsRoad1[0]);
		int street2 = Traffic.getAvenueOrStreetNumber(partsRoad2[0]);
		if(street1 == street2)
		{
			return partsRoad1[0].trim();
		}
		else
		{
			int av1 = Traffic.getAvenueOrStreetNumber(partsRoad1[1]);
			int av2 = Traffic.getAvenueOrStreetNumber(partsRoad2[1]);
			if(av1 == av2)
				return partsRoad1[1].trim();
			else
				return null;
		}
	}
	
	public static String[] getUncommonRoads(String road1, String road2, String commonPart)
	{
		String[] uncommon = new String[2];
		String[] partsRoad1 = road1.split("[+]");
		String[] partsRoad2 = road2.split("[+]");
		
		if(commonPart != null)
		{
			if(partsRoad1[0].trim().compareTo(commonPart.trim()) == 0)
			{
				uncommon[0] = partsRoad1[1].trim();
				uncommon[1] = partsRoad2[1].trim();
			}
			else
			{
				uncommon[0] = partsRoad1[0].trim();
				uncommon[1] = partsRoad2[0].trim();
			}
		}
		return uncommon;
	}
	
	//adding delay to particular step.
	public static void addDelay(RouteInfo.maneuvar step)
	{
		int delay = rand.nextInt(10);
		System.out.println("random delay:" + delay * 60);
	}
}