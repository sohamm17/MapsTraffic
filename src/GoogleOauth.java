import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import javax.json.*;
import javax.json.stream.JsonParser;
import javax.json.Json;

import com.google.api.client.auth.oauth2.AccessTokenErrorResponse;
import com.google.api.client.auth.oauth2.AccessTokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;


public class GoogleOauth {
	
	public static double speed;
	public String retrieve(String code) throws IOException {
		String token = "";
		String refresh_token = "";
		  try {
			  System.out.println("Test Print");
		    GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
		    		new NetHttpTransport(),
		    		new JacksonFactory(),
		        "947782242448-sq1cakbpuapr6qgmnqqu9trkavn25q9k.apps.googleusercontent.com",
		        "afSCZ9JfdJhHcQ9vMn-JNjhp",
		        "4/kd8LZ3iy7Uixs_pbJOcAGV8hkBVGmvmUeudBr4fjq8Q.EtlonwDsdYEQEnp6UAPFm0FkJo6qmAI",
		        "https://developers.google.com/oauthplayground").execute();
		    System.out.println("test print 2");
		    
		    System.out.println("Access token: " + response.getAccessToken());
		    System.out.println("Refresh token: " + response.getRefreshToken());
		    token = response.getAccessToken().toString();
		   // refresh_token = GoogleOauth.requestAccessToken(response.getRefreshToken());
		  } 
		  
		  catch (TokenResponseException e) {
		      if (e.getDetails() != null) {
		        System.err.println("Error: " + e.getDetails().getError());
		        if (e.getDetails().getErrorDescription() != null) {
		          System.err.println(e.getDetails().getErrorDescription());
		        }
		        if (e.getDetails().getErrorUri() != null) {
		          System.err.println(e.getDetails().getErrorUri());
		        }
		      } else {
		        System.err.println(e.getMessage());
		      }
		    }
		  return token;
	}
	
/*	static String requestAccessToken(String refreshToken) throws IOException {
		 String refresh_token = "";
		   try {
		     RefreshAccessTokenRequest request = new RefreshAccessTokenRequest(null);
		     request.clientId = "947782242448-sq1cakbpuapr6qgmnqqu9trkavn25q9k.apps.googleusercontent.com";
		     request.clientSecret = "afSCZ9JfdJhHcQ9vMn-JNjhp";
		     request.refreshToken = refreshToken;
		     AccessTokenResponse response =
		         request.execute().parseAs(AccessTokenResponse.class);
		     System.out.println("Access token: " + response.accessToken);
		     refresh_token = response.accessToken;
		     
		   } 
		   catch (HttpResponseException e) {
		   //  AccessTokenErrorResponse response =
		     //    e. parseAs(AccessTokenErrorResponse.class);
		     System.out.println("Error: " + e);
		   }
		   return refresh_token;
		 } */
		 
	
	public double GoogleFitApi(String token) throws URISyntaxException
	{
		try 
		{
			//String url = "https://www.googleapis.com/fitness/v1/users/me/dataSources/" +
				//	"derived:com.google.speed:com.google.android.gms:motorola:XT1034:516693f8:/" +
					//"datasets/1427023121000000000-1427087921000000000";
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			httpClient = HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
			URIBuilder builder = new URIBuilder()
			.setScheme("https")
			.setHost("www.googleapis.com")
			.setPath("/fitness/v1/users/me/dataSources/derived:com.google.speed:com.google.android.gms:motorola:XT1034:516693f8:/datasets/1427023121000000000-1427087921000000000");
			
				//.addParameter("Access_Token", "ya29.QQGQMPSy9zc0GJgjDfV6EhcfEAvAdf79zCDLebh29kjR967oUdbL5jKpAYTSqnjnctsq-FmGzd-C8g");
            HttpGet request = new HttpGet(builder.build());
            System.out.println(request);
            
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", "Bearer " + "ya29.QgElVnyMRltIkdTibnKTGW9f5NQTG0znEfC59s-t1Mx56lqTyYrGu0qU5ZdNChcBY6a2ChbHicTQog");
            HttpResponse result = httpClient.execute(request);
            System.out.println(result);

            String json = EntityUtils.toString(result.getEntity(), "UTF-8");
            //System.out.println(json);
            
            StringReader reader = new StringReader(json);
            JsonReader jsonReader = Json.createReader(reader);
            JsonObject wholeObject = jsonReader.readObject();
            
            JsonArray points = (JsonArray) wholeObject.get("point");
            //System.out.println(points);
            
            for(JsonValue point : points)
            {
            	JsonObject eachrow = (JsonObject) point;
            	JsonArray pt = eachrow.getJsonArray("value");
            	//System.out.println(pt);
            	
            	for (JsonValue p : pt)
            	{
            		JsonObject checknow = (JsonObject) p;
            		String check = checknow.get("fpVal").toString();
                	if(Double.parseDouble(check)<1.0)
                	{
                	 speed = Double.parseDouble(check);
                	}
            		
            	}

            }          
            
	}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
	return speed;
}
}