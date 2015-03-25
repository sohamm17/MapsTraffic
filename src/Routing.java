import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;

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
	String APP_ID = "DemoAppId01082013GAL";
	String APP_CODE = "AJKnXv84fjrb0KIHawS0Tg";
	
	public void getRoute(String source_string, String destination_string) throws URISyntaxException
	{
		String url = "http://geocoder.cit.api.here.com/6.2/geocode.json";
		
		URIBuilder builder = new URIBuilder(url)
			.addParameter("app_id", APP_ID)
			.addParameter("app_code", APP_CODE)
			.addParameter("gen", "8")
			.addParameter("searchtext", source_string)
			;
		
		System.out.println(builder.build());
		
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
		{
		
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
	            
	            JsonObject firstView = (JsonObject) wholeObject.getJsonObject("Response").getJsonArray("View").get(0);
	            
	            JsonObject firstResult = (JsonObject) firstView.getJsonArray("Result").get(0);
	            
	            JsonObject displayPosition = firstResult.getJsonObject("Location").getJsonObject("DisplayPosition");
	            
	            //System.out.println(displayPosition);
	            System.out.println(displayPosition.getJsonNumber("Latitude"));
	            System.out.println(displayPosition.getJsonNumber("Longitude"));
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
	
}
