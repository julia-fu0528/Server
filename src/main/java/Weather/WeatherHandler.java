package Weather;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.concurrent.TimeUnit;
import spark.Request;
import spark.Response;
import spark.Route;

public class WeatherHandler implements Route {

    private WeatherCachingProxy cache;

    public WeatherHandler() {
        this.cache = new WeatherCachingProxy(30,20, TimeUnit.MINUTES,0.7);
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        try {
            if (request.queryParams("lat") == null || request.queryParams("lon") == null) {
                return new WeatherFailureResponse("error_bad_request", "Missing \"lon\" or \"lat\" field.")
                        .serialize();
            }
            double lat = Double.parseDouble(request.queryParams("lat"));
            double lon = Double.parseDouble(request.queryParams("lon"));

            //calls weather api, determines which grid point the cords are in
            Data.Forecast weatherF = this.cache.getForecast(lat, lon);



            if (weatherF == null) {
                return new WeatherFailureResponse("error_datasource",
                        "No available weather data at the provided location.").serialize();
            }

            return new WeatherSuccessResponse(weatherF.temp(), weatherF.unit(),
                    this.cache.getCurrentTime()).serialize();



//
//            if (gridResponse.properties() != null) {
//                String weatherURL = gridResponse.properties().endpoint();
//                WeatherResponse wResponse = this.data.getURLWeatherResponse(weatherURL);
//                if (wResponse.properties() == null) {
//                    return new WeatherFailureResponse("error_datasource", "Weather data was not available " +
//                            "for the requested point").serialize();
//                }
//                Forecast forecast = wResponse.properties().periods().get(0);
//                return new WeatherSuccessResponse(forecast.temp(), forecast.unit(), data.getCurrentTime()).serialize();
//            } else {
//                //TODO: change
//                if (gridResponse.coordinates().equals("Data Unavailable For Requested Point")) {
//                    return new WeatherFailureResponse("error_bad_request", "The request point does not appear" +
//                            "to be a valid coordinate").serialize();
//                    //TODO: change
//                } else if (gridResponse.coordinates().equals("Invalid Parameter")) {
//                    return new WeatherFailureResponse(
//                            "error_bad_request",
//                            "The request point does not appear to be a valid coordinate.")
//                            .serialize();
//                } else {
//                    return new WeatherFailureResponse(
//                            "error_internal",
//                            "Received bad response from the grid endpoint. "
//                                    + "This will be logged internally and investigated.")
//                            .serialize();
//                }
//            }
        } catch (NumberFormatException e) {
            return new WeatherFailureResponse(
                    "error_bad_request", "Failed to parse number values from the request point.")
                    .serialize();
        }  catch (Exception e) {
            return new WeatherFailureResponse(
                    "error_internal",
                    "Unexpected error. This will be logged internally and investigated.")
                    .serialize();
        }
    }

    public record WeatherFailureResponse(String result, String message) {
        String serialize() {
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(WeatherFailureResponse.class).toJson(this);
        }
    }

    public record WeatherSuccessResponse(String result, Double temperature, String unit, String timeRetrieved) {
        public WeatherSuccessResponse(Double temperature, String unit, String timeRetrieved) {
            this("success", temperature, unit, timeRetrieved);
        }

        /**
         * Serializes the response into JSON.
         *
         * @return serialized response as a JSON string.
         */
        String serialize() {
            try {
                Moshi moshi = new Moshi.Builder().build();
                return moshi.adapter(WeatherSuccessResponse.class).toJson(this);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }
}