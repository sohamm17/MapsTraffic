import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;


public class Routing 
{
	static String APP_ID = "DemoAppId01082013GAL";
	static String APP_CODE = "AJKnXv84fjrb0KIHawS0Tg";
	
	public void getRoute(String source_string, String destination_string, double walkSpeed) throws URISyntaxException
	{
		double[] src_latlon = new double[2], dst_latlon = new double[2];
		
		//Getting latitude and longitude of source and destination
		getLatLong(source_string, src_latlon);
		getLatLong(destination_string, dst_latlon);
		
		System.out.println(src_latlon[0] + "\t" + src_latlon[1]);
		System.out.println(dst_latlon[0] + "\t" + dst_latlon[1]);
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		
		//Calculating the route
		String url = "http://route.cit.api.here.com/routing/7.2/calculateroute.json";
		
	
		URIBuilder builder = new URIBuilder(url)
		.addParameter("app_id", APP_ID)
		.addParameter("app_code", APP_CODE)
		.addParameter("waypoint0", "geo!" + getAngleAsString(src_latlon))
		.addParameter("waypoint1", "geo!" + getAngleAsString(dst_latlon))
		.addParameter("mode", "fastest;publicTransportTimeTable") //Getting public routes with the fastest options
		.addParameter("combineChange", "true")
		.addParameter("alternatives", "2") //getting 2 alternative routes
		.addParameter("walkSpeed", String.valueOf(walkSpeed)) //allowed value between 0.5 to 2
		.addParameter("departure", dateFormat.format(date))
		;
		
		System.out.println(builder.build());
	}
	
	//Given a location it gets the latitude and longitude and returns them through the array latlong
	//lat is at 0th index, lon is at 1st.
	public static void getLatLong(String location, double[] latlon) throws URISyntaxException
	{
		String url = "http://geocoder.cit.api.here.com/6.2/geocode.json";
		
		URIBuilder builder = new URIBuilder(url)
			.addParameter("app_id", APP_ID)
			.addParameter("app_code", APP_CODE)
			.addParameter("gen", "8")
			.addParameter("searchtext", location)
			;
		
		System.out.println(builder.build());
		
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
		{
		
			HttpGet request = new HttpGet(builder.build());
	        
	        request.addHeader("content-type", "application/json");
	        HttpResponse result = httpClient.execute(request);
	
	        String json = EntityUtils.toString(result.getEntity(), "UTF-8");
	        //System.out.println(json);
	        try
	        {
	        	StringReader reader = new StringReader(json);
	            JsonReader jsonReader = Json.createReader(reader);
	            JsonObject wholeObject = jsonReader.readObject();
	            
	            JsonObject firstView = (JsonObject) wholeObject.getJsonObject("Response").getJsonArray("View").get(0);
	            
	            JsonObject firstResult = (JsonObject) firstView.getJsonArray("Result").get(0);
	            
	            JsonObject displayPosition = firstResult.getJsonObject("Location").getJsonObject("DisplayPosition");
	            
	            //System.out.println(displayPosition);
	            latlon[0] = displayPosition.getJsonNumber("Latitude").doubleValue();
	            latlon[1] = displayPosition.getJsonNumber("Longitude").doubleValue();
	        }
	        catch (Exception e)
	        {
	        	System.err.println("Error: " + e.getMessage());
	        	e.printStackTrace();
	        }
		}
		catch (IOException ex) 
        {
			System.err.println("Error: " + ex.getMessage());
        }
	}
	
	//Getting latitude or longitude as string form i.e separated by commas
	private static String getAngleAsString(double[] angle)
	{
		return angle[0] + "," + angle[1];
	}
}
