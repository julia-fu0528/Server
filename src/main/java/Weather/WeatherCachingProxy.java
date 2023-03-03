package Weather;

import Weather.Requester.PlainRequester;
import Weather.Requester.Requester;
import Weather.Data.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.time.format.DateTimeFormatter;


/**
 * proxy, caches weather requests and handles updating the state (like ServerData, however, specific to weather)
 */
public class WeatherCachingProxy {

    private final LoadingCache<List<Double>, WeatherResponse> cache;
    private final double distance;
    private WeatherResponse weatherResponse;

    /**
     * Creates a requestor object–mockRequestor for mocking, otherwise PlainRequestor
     */
    private Requester requester;

    /**
     * Proxy for the weather cache - initializes the cache with parameters size, expirationTime, and requestor
     * @param maxSize – the maximum size of the cache. In this case, 100.
     * @param expirationTime – the maximum amount of that an element an remain in the cache unqueried. In this case,
     *                       10 mins
     * @param requester – the requestor object that is being used. In the case of mocking, this is not a real restore.
     *                  However, it does work with the weather API requester.
     *
     *
     *
     */

    /**
     * Description:
     * This method creates a caching proxy that wraps around the requester instance variable. When the
     * requestToInstantiate() method is called with a new URL, it first checks if the requested URL is present in the
     * cache. If it is, it returns the cached value immediately without making a new request. If the requested URL is
     * not present in the cache, the requestToInstantiate() method is called on the requester instance variable to
     * make a new request and get the response. The response is then added to the cache so that subsequent requests for
     * the same URL can be returned from the cache without making new requests.
     * @param maxSize
     * @param expirationTime
     */

    public WeatherCachingProxy(int maxSize, int expirationTime, TimeUnit tUnit, Double distance) {
        this.distance = distance;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(expirationTime, tUnit)
                .build(
                        new CacheLoader<>() {
                            @NotNull
                            @Override
                            public WeatherResponse load(@NotNull List<Double> list) throws Exception {
                                String gridUrl = "https://api.weather.gov/points/" + list.get(0) + "," + list.get(1);

                                PlainRequester weatherRequester = new PlainRequester();
                                GridResponse gridResponse = weatherRequester.requestToInstantiate(gridUrl,GridResponse.class);
                                String weatherUrl = gridResponse.properties().endpoint();
                                WeatherResponse weatherResponse = weatherRequester.requestToInstantiate(weatherUrl,WeatherResponse.class);

                                return weatherResponse;
                            }
                        });
    }


    public Forecast getForecast(double lat, double lon) throws ExecutionException {
        // calculate distance and determine cache-matching based on developer preference
        for (List<Double> coordinates: this.cache.asMap().keySet()) {

            double distance = Math.sqrt(Math.pow(Math.abs(lat - coordinates.get(0)), 2) +
                    Math.pow(Math.abs(lon - coordinates.get(1)), 2));


            if (distance <= this.distance) {
                this.weatherResponse = this.cache.getUnchecked(coordinates);
                return this.cache.getUnchecked(coordinates).properties().periods().get(0);
            }
        }
        List<Double> cachingCoordinates = new ArrayList<>();
        cachingCoordinates.add(lat);
        cachingCoordinates.add(lon);
        this.weatherResponse = this.cache.getUnchecked(cachingCoordinates);
        for (List<Double> key: this.cache.asMap().keySet()) {
            System.out.println(this.cache.get(key) + "\n");
        }
        return this.cache.getUnchecked(cachingCoordinates).properties().periods().get(0);
    }

    public WeatherResponse getWeatherResponse() {
        return this.weatherResponse;
    }

    /**
     * Returns the current time using DataTimeFormatter and LocalDateTime
     * @return – the current day and time in the format yyyy/MM/dd HH:mm:ss
     */
    public String getCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return formatter.format(now);
    }

    /**
     * Function that returns the current state of the cache using a concurrentMap object
     * @return a cache map that is the currently cached values
     */
    public ConcurrentMap<List<Double>, WeatherResponse> getCacheState() {
        return this.cache.asMap();
    }
}