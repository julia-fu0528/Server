package Handlers;

import com.squareup.moshi.Moshi;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherHandler implements Route {
    public Object handle(Request request, Response response) throws Exception {
        String latitude = request.queryParams("lat");
        String longitude = request.queryParams("lon");
        if (latitude == null) {
            return new MissingLatitudeResponse().serialize();
        } else if (longitude == null) {
            return new MissingLongitudeResponse().serialize();
        }
        float lat_float;
        float lon_float;
        try {
            lat_float = Float.parseFloat(latitude);
            lon_float = Float.parseFloat(longitude);
        } catch (NumberFormatException e) {
            return new WrongCoorFormatResponse(latitude, longitude).serialize();
        }
        HttpResponse<String> coordinateResponse =
                get_apiResponse("https://api.weather.gov/points/" + latitude + "," + longitude);

        int status_code = coordinateResponse.statusCode();

        if (status_code == 404) {
            return new NWSNoDataResponse(lat_float, lon_float).serialize();
        } else if (status_code == 400) {
            return new InvalidLocationResponse(lat_float, lon_float).serialize();
        } else if (status_code == 500) {
            return new InternalErrorResponse(lat_float, lon_float).serialize();
        }
        // else if (status_code == 200) {
        return new CoordinateSuccessResponse(coordinateResponse, lat_float, lon_float).serialize();
    }
    public static HttpResponse<String> get_apiResponse(String website) throws IOException, InterruptedException, URISyntaxException{
        HttpRequest request = HttpRequest.newBuilder().uri(new URI(website)).GET().build();
        return HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
    }
//    public static String get_locationResponse(HttpResponse<String> response, float lat_float, float lon_float) throws Exception{
//        Moshi moshi = new Moshi.Builder().build();
//        NWSPoints points = moshi.adapter(NWSPoints.class).fromJson(response.body());
//    }
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
    public record NWSNoDataResponse(String response_type, Float lat_float, Float lon_float, String message){
        public NWSNoDataResponse(Float lat_float, Float lon_float){
            this("error_json", lat_float, lon_float,
                    "No data exist for coordinates (" + lat_float + "," + lon_float + ")");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(NWSNoDataResponse.class).toJson(this);
        }
    }
    public record InvalidLocationResponse(String response_type, Float lat_float, Float lon_float, String message){
        public InvalidLocationResponse(Float lat_float, Float lon_float){
            this("error_json", lat_float, lon_float,
                    "Invalid latitude" + lat_float + "and longitutd" + lon_float);
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(InvalidLocationResponse.class).toJson(this);
        }
    }
    public record InternalErrorResponse(String response_type, Float lat_float, Float lon_float, String message){
        public InternalErrorResponse(Float lat_float, Float lon_float){
            this("error_json", lat_float, lon_float,
                    "Coordinates (" + lat_float + "," + lon_float + ") causes internal error");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(InternalErrorResponse.class).toJson(this);
        }
    }
    public record CoordinateSuccessResponse(String response_type, HttpResponse<String> coordinateResponse, Float lat_float, Float lon_float, String message){
        public CoordinateSuccessResponse(HttpResponse<String> coordinateResponse, Float lat_float, Float lon_float){
            this("success", coordinateResponse, lat_float, lon_float,
                    "Weather information successfully obtained.");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(CoordinateSuccessResponse.class).toJson(this);
        }
    }
}
