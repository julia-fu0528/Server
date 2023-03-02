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
    /**
     * handles weather request
     * @param request the request to handle
     * @param response use to modify properties of the response
     * @return weather condition at the given latitude and longitutde
     * @throws Exception if the latitude and longitude are not floats or when api status errors occur
     */
    @Override
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
                getApiInfo("https://api.weather.gov/points/" + latitude + "," + longitude);

        int status_code = coordinateResponse.statusCode();

        if (status_code == 404) {
            return new NWSNoDataResponse(lat_float, lon_float).serialize();
        } else if (status_code == 400) {
            return new InvalidLocationResponse(lat_float, lon_float).serialize();
        } else if (status_code == 500) {
            return new InternalErrorResponse(lat_float, lon_float).serialize();
        }
        // else if (status_code == 200) {
        return getLocationInfo(coordinateResponse, lat_float, lon_float);
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Records to keep track of information from NWS
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Record for a specific point with latitude and longitude
     * @param properties the weather properties of such a location
     */
    public record NWSCoordinates(NWSCoorProps properties) {}

    /**
     * The properties of a location
     * @param hourly_forecast a string containing information about the hourly forcast of weather in this location
     */
    public record NWSCoorProps(String hourly_forecast) {}
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Temperature forecast
     * @param properties the HWSHourlyProps type properties of temperature forecast
     */
    public record NWSForecast(NWSHourlyProps properties) {}

    /**
     * Properties of temperature forecast
     * @param period the NWSPeriod type list of time period for temperature forecast
     */
    public record NWSHourlyProps(NWSPeriod[] period) {}

    /**
     * Time period for temperature forecast
     * @param temperature the integer temperature for this period
     */
    public record NWSPeriod(int temperature) {}
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper functions
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * To get api response from NWS website
     * @param website the url of the NWS, with latitude and longitude
     * @return the String response from NWS
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public static HttpResponse<String> getApiInfo(String website) throws IOException, InterruptedException, URISyntaxException{
        HttpRequest request = HttpRequest.newBuilder().uri(new URI(website)).GET().build();
        return HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * To get forecast about a location
     * @param response string response extracting information of a location
     * @param lat_float latitude of the location
     * @param lon_float longitude of the location
     * @return String weather forecast
     * @throws Exception
     */
    public static String getLocationInfo(HttpResponse<String> response, float lat_float, float lon_float) throws Exception{
        Moshi moshi = new Moshi.Builder().build();
        NWSCoordinates points = moshi.adapter(NWSCoordinates.class).fromJson(response.body());
        if (points.properties().hourly_forecast() == null){
            return new InternalErrorResponse(lat_float, lon_float).serialize();
        }
        HttpResponse<String> api_response = getApiInfo(points.properties.hourly_forecast());
        int status_code = api_response.statusCode();
        if (status_code == 404) {
            return new NWSNoDataResponse(lat_float, lon_float).serialize();
        }else if (status_code == 500) {
            return new InternalErrorResponse(lat_float, lon_float).serialize();
        }
        NWSForecast nws_forecast = moshi.adapter(NWSForecast.class).fromJson(response.body());
        return getForecast(nws_forecast, lat_float, lon_float);
    }

    /**
     * To get Json temperature forecast of a location
     * @param forecast weather information
     * @param lat latitude of the location
     * @param lon longitude of the location
     * @return Json forecast
     * @throws Exception
     */
    public static String getForecast(NWSForecast forecast, float lat, float lon) {
        int temp = forecast.properties().period()[0].temperature();
        return new ForecastSuccessResponse(lat, lon, temp).serialize();
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Responses
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
    public record NWSNoDataResponse(String response_type, Float lat, Float lon, String message){
        public NWSNoDataResponse(Float lat, Float lon){
            this("error_json", lat, lon,
                    "No data exist for coordinates (" + lat + "," + lon + ")");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(NWSNoDataResponse.class).toJson(this);
        }
    }
    public record InvalidLocationResponse(String response_type, Float lat, Float lon, String message){
        public InvalidLocationResponse(Float lat, Float lon){
            this("error_json", lat, lon,
                    "Invalid latitude " + lat+ "and longitude " + lon);
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(InvalidLocationResponse.class).toJson(this);
        }
    }
    public record InternalErrorResponse(String response_type, Float lat, Float lon, String message){
        public InternalErrorResponse(Float lat, Float lon){
            this("error_json", lat, lon,
                    "Coordinates (" + lat + "," + lon + ") causes internal error");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(InternalErrorResponse.class).toJson(this);
        }
    }
    public record ForecastSuccessResponse(String response_type, float lat, float lon, int temp, String message){
        public ForecastSuccessResponse(float lat, float lon, int temp){
            this("success", lat, lon, temp, "Forecast successfully retrieved from NWS");
        }
        String serialize(){
            try{
                Moshi moshi = new Moshi.Builder().build();
                return moshi.adapter(ForecastSuccessResponse.class).toJson(this);
            }
            catch(Exception e){
                e.printStackTrace();
                throw e;
            }
        }
    }
}
