package Weather;

import com.squareup.moshi.Json;
import java.util.List;

/** Articulates the forms in which the JSON data received from the weather API are parsed into. */
public class Data {

    /** Wrapper for forecast of each period in ForecastProperties. */
    public record Forecast(
            @Json(name = "temperature") double temp, @Json(name = "temperatureUnit") String unit) {}

    /** Wrapper for properties attribute of weather JSON. */
    public record ForecastProperties(@Json(name = "periods") List<Forecast> periods) {}

    /** Wrapper for weather URL response. */
    public record WeatherResponse(@Json(name = "properties") ForecastProperties properties) {}

    /** Wrapper for grid point URL response. */
    public record GridResponse(
            @Json(name = "properties") GridProperties properties, @Json(name = "title") String title) {}

    /** Wrapper for properties attribute of grid point JSON. */
    public record GridProperties(@Json(name = "forecast") String endpoint) {}
}
