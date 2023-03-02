package Handlers;

import com.squareup.moshi.Moshi;
import spark.Request;
import spark.Response;
import spark.Route;

import java.net.http.HttpResponse;

public class WeatherHandler implements Route {
    public Object handle(Request request, Response response)throws Exception{
        String latitude = request.queryParams("lat");
        String longitude = request.queryParams("lon");
        if (latitude == null){
            return new MissingLatitudeResponse().serialize();
        }else if(longitude == null){
            return new MissingLongitudeResponse().serialize();
        }
        try{
            float lat_float = Float.parseFloat(latitude);
            float lon_float = Float.parseFloat(longitude);
        }catch(NumberFormatException e){
            return new WrongCoorFormatResponse(latitude, longitude).serialize();
        }
        HttpResponse<String> coordinateResponse =
                get_apiResponse("https://api.weather.gov/points/" + latitude + "," + longitude);
    }
    public static HttpResponse<String> getAPI
    public record MissingLatitudeResponse(String response_type, String message){
        public MissingLatitudeResponse(){
            this("error_datasource", "Missing latitude query");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(MissingLatitudeResponse.class).toJson(this);
        }
    }
    public record MissingLongitudeResponse(String response_type, String message) {
        public MissingLongitudeResponse() {
            this("error_datasource", "Missing longitude query");
        }

        String serialize() {
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(MissingLongitudeResponse.class).toJson(this);
        }
    }
    public record WrongCoorFormatResponse(String response_type, String lat, String lon, String message){
        public WrongCoorFormatResponse(String lat, String lon){
            this("error_json", lat, lon,
                    "Lat'"+lat+ "'and lon'"+lon+"' should be floats.");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(WrongCoorFormatResponse.class).toJson(this);
        }
    }
}
