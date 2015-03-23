
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

	static final String PUBLIC_API_KEY = "AIzaSyBNJndqJCWHbRrZ4DbGzWEQ19ROJYfM8wg";
	static Random rand;

	public static void main(String[] args) throws Exception {
		
		MapsTrafficMain myMap = new MapsTrafficMain();
		//myMap.hereMapsAPI();
		
		rand = new Random(50);// giving seed value to get finite result everytime
		GeoApiContext context = new GeoApiContext().setApiKey(PUBLIC_API_KEY);
		
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
	}
	
	public void hereMapsAPI() throws URISyntaxException
	{
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
		{
			String url = "http://traffic.cit.api.here.com/traffic/6.1/flow.json";
			
			URIBuilder builder = new URIBuilder(url)
				.addParameter("app_id", "DemoAppId01082013GAL")
				.addParameter("app_code", "AJKnXv84fjrb0KIHawS0Tg")
				.addParameter("bbox", "53.5145554,-113.5103963;53.5197864,-113.5212539");
			
            HttpGet request = new HttpGet(builder.build());
            
            
            request.addHeader("content-type", "application/json");
            HttpResponse result = httpClient.execute(request);

            String json = EntityUtils.toString(result.getEntity(), "UTF-8");
            System.out.println(json);
            try
            {
            	StringReader reader = new StringReader(json);
                JsonReader jsonReader = Json.createReader(reader);
                JsonObject wholeObject = jsonReader.readObject();
                
                JsonArray RoadWays = (JsonArray) wholeObject.get("RWS");
                
                for(JsonValue eachRWS : RoadWays)
                {
                	JsonObject eachRWSObject = (JsonObject) eachRWS;
                	JsonArray Roads = eachRWSObject.getJsonArray("RW");
                	
                	for(JsonValue eachRoad : Roads)
                	{
                		JsonObject eachRoadObject = (JsonObject) eachRoad;
                		
                		System.out.println(eachRoadObject.getString("DE"));
                		
                		JsonArray flowItemElements = eachRoadObject.getJsonArray("FIS");
                		
                		for(JsonValue eachFlowItemElement : flowItemElements)
                		{
                			JsonArray singleFlowItems = ((JsonObject)eachFlowItemElement).getJsonArray("FI");
                			
                			for(JsonValue eachSingleFlowItem: singleFlowItems)
                			{
                				JsonObject singleFlowTMC = ((JsonObject)eachSingleFlowItem).getJsonObject("TMC");
                				System.out.print(singleFlowTMC.getString("DE") + "-" + singleFlowTMC.getJsonNumber("LE") + " miles");
                				
                				JsonArray currentFlows = ((JsonObject)eachSingleFlowItem).getJsonArray("CF");
                				
                				for(JsonValue eachCurrentFlow : currentFlows)
                				{
                					JsonObject eachCurrentFlowObject = ((JsonObject)eachCurrentFlow);
                					System.out.println(" --> FreeFlow: " + eachCurrentFlowObject.getJsonNumber("FF")
                							+ ", Speed(by limit): " + eachCurrentFlowObject.getJsonNumber("SP")
                							+ ", Speed(not limit): " + eachCurrentFlowObject.getJsonNumber("SU")
                							+ ", Jam Factor: " + eachCurrentFlowObject.getJsonNumber("JF")
                							+ ", Confidence: " + eachCurrentFlowObject.getJsonNumber("CN")
                							);
                				}
                			}
                		}
                		System.out.println();
                	}
                }
            } 
            catch (Exception e)
            {
            	System.err.println("Error: " + e.getMessage());
            	e.printStackTrace();
            }

        }
		catch (IOException ex) 
        {
        }
	}
	
	//adding delay to particular step.
	public static void addDelay(DirectionsStep step)
	{
		int delay = rand.nextInt(10);
		System.out.println("random delay:" + delay * 60);
		step.duration.inSeconds += delay *60;
	}
}