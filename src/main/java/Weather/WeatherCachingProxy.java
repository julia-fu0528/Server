package Weather;

import Weather.Requester.Requestor;
import Weather.Data.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/** Proxy for the caching weather requests. */
public class WeatherCachingProxy {

    /** Cache for storing WeatherResponse instances. */
    private final LoadingCache<String, Data.WeatherResponse> cache;

    /** Requestor object used for mocking. */
    private final Requestor requestor;

    /**
     * Constructs a WeatherServerState object.
     *
     * @param requestor an object to query resources with.
     */
    public WeatherCachingProxy(Requestor requestor) {
        this(100, 10, requestor);
    }

    /**
     * Initializes the cache from the specified parameters.
     *
     * @param maxSize maximum size of the cache.
     * @param duration maximum duration after which the value will expire if there were no access
     *     attempts.
     * @param requestor an object to query resources with.
     */
    public WeatherCachingProxy(int maxSize, int duration, Requestor requestor) {
        this.requestor = requestor;
        this.cache =
                CacheBuilder.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterAccess(duration, TimeUnit.MINUTES)
                        .build(
                                new CacheLoader<>() {
                                    @NotNull
                                    @Override
                                    public WeatherResponse load(@NotNull String url) throws Exception {
                                        return requestor.requestAndInstantiate(url, WeatherResponse.class);
                                    }
                                });
    }

    /**
     * Returns the weather data retrieved from the specified URL using caching.
     *
     * @param url url to retrieve weather data from.
     * @return weather data retrieved from api/cache.
     */
    public WeatherResponse getWeatherResponseByUrl(String url) throws ExecutionException {
        return this.cache.get(url);
    }

    /**
     * Returns the grid point data retrieved from the specified URL.
     *
     * @param url url to retrieve gridpoint data from.
     * @return gridpoint data retrieved from api.
     */
    public GridResponse getGridResponseByUrl(String url) throws IOException {
        return this.requestor.requestAndInstantiate(url, GridResponse.class);
    }

    /**
     * A method to retrieve the current time.
     *
     * @return the current time formatted in yyyy/MM/dd HH:mm:ss
     */
    public String getCurrentTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    /**
     * Returns current cache state for testing purposes.
     *
     * @return raw cache map representing the cached values.
     */
    public ConcurrentMap<String, Data.WeatherResponse> getCacheMap() {
        return cache.asMap();
    }
}