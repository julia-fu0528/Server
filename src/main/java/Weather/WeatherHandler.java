package Weather;

import Weather.WeatherCachingProxy;
import Weather.Data.GridResponse;
import Weather.Data.*;
import com.squareup.moshi.Moshi;
import spark.Request;
import spark.Response;
import spark.Route;

/** Handles requests to get forecast data from coordinate. */
public class WeatherHandler implements Route {

    /** Proxy for caching. */
    private final WeatherCachingProxy state;

    /**
     * Constructs a WeatherHandler with a given state.
     *
     * @param state caching proxy.
     */
    public WeatherHandler(WeatherCachingProxy state) {
        this.state = state;
    }

    /**
     * Handles a search request.
     *
     * @param request a Spark request.
     * @param response a Spark response.
     * @return a response object, either WeatherSuccessResponse or WeatherFailureResponse.
     */
    @Override
    public Object handle(Request request, Response response) throws Exception {
        try {
            if (request.queryParams("lat") == null) {
                return new WeatherFailureResponse("error_bad_request", "Missing \"lat\" field.")
                        .serialize();
            }
            if (request.queryParams("lon") == null) {
                return new WeatherFailureResponse("error_bad_request", "Missing \"lon\" field.")
                        .serialize();
            }
            double lat = Double.parseDouble(request.queryParams("lat"));
            double lon = Double.parseDouble(request.queryParams("lon"));

            // calls weather api to determine which grid point the coordinates are in
            String gridUrl = "https://api.weather.gov/points/" + lat + "," + lon;
            GridResponse gridResponse = state.getGridResponseByUrl(gridUrl);

            // if the grid point is unable to be located
            if (gridResponse.properties() != null) {
                String weatherUrl = gridResponse.properties().endpoint();
                WeatherResponse weatherResponse = this.state.getWeatherResponseByUrl(weatherUrl);
                if (weatherResponse.properties() == null) {
                    return new WeatherFailureResponse(
                            "error_datasource", "The weather data was unavailable for the requested point.")
                            .serialize();
                }
                Forecast forecast = weatherResponse.properties().periods().get(0);
                return new WeatherSuccessResponse(forecast.temp(), forecast.unit(), state.getCurrentTime())
                        .serialize();
            } else {
                if (gridResponse.title().equals("Data Unavailable For Requested Point")) {
                    return new WeatherFailureResponse(
                            "error_datasource", "The grid data was unavailable for the requested point.")
                            .serialize();
                } else if (gridResponse.title().equals("Invalid Parameter")) {
                    return new WeatherFailureResponse(
                            "error_bad_request",
                            "The request point does not appear to be a valid coordinate.")
                            .serialize();
                } else {
                    return new WeatherFailureResponse(
                            "error_internal",
                            "Received bad response from the grid endpoint. "
                                    + "This will be logged internally and investigated.")
                            .serialize();
                }
            }
        } catch (NumberFormatException e) {
            return new WeatherFailureResponse(
                    "error_bad_request", "Failed to parse number values from the request point.")
                    .serialize();
        } catch (Exception e) {
            return new WeatherFailureResponse(
                    "error_internal",
                    "Unexpected error. This will be logged internally and investigated.")
                    .serialize();
        }
    }

    /**
     * Response returned in case the weather request was successful.
     *
     * @param result result of the operation.
     * @param temperature temperature.
     * @param unit unit of temperature.
     * @param timeRetrieved time the data was retrieved from weather api.
     */
    public record WeatherSuccessResponse(
            String result, Double temperature, String unit, String timeRetrieved) {

        /**
         * Constructs a WeatherSuccessResponse.
         *
         * @param temperature temperature.
         * @param unit unit of temperature.
         * @param timeRetrieved time the data was retrieved from weather api.
         */
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

    /**
     * Response returned in case the weather request was unsuccessful.
     *
     * @param result result of the operation.
     * @param reason reason why request errored.
     */
    public record WeatherFailureResponse(String result, String reason) {

        /**
         * Serializes the response into JSON.
         *
         * @return serialized response as a JSON string.
         */
        String serialize() {
            try {
                Moshi moshi = new Moshi.Builder().build();
                return moshi.adapter(WeatherFailureResponse.class).toJson(this);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }
}