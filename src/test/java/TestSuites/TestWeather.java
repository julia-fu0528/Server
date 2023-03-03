package TestSuites;

import static org.junit.jupiter.api.Assertions.*;

import Handlers.LoadHandler;
import Handlers.SearchHandler;
import Handlers.ViewHandler;
import Servers.LoadedFiles;
import Weather.WeatherCachingProxy;
import Weather.WeatherHandler;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;

public class TestWeather {

    private WeatherCachingProxy cache;

    @BeforeAll
    public static void setup_before_everything() {

        // Set the Spark port number. This can only be done once, and has to
        // happen before any route maps are added. Hence using @BeforeClass.
        // Setting port 0 will cause Spark to use an arbitrary available port.
        Spark.port(0);
        // Don't try to remember it. Spark won't actually give Spark.port() back
        // until route mapping has started. Just get the port number later. We're using
        // a random _free_ port to remove the chances that something is already using a
        // specific port on the system used for testing.

        // Remove the logging spam during tests
        //   This is surprisingly difficult. (Notes to self omitted to avoid complicating things.)

        // SLF4J doesn't let us change the logging level directly (which makes sense,
        //   given that different logging frameworks have different level labels etc.)
        // Changing the JDK *ROOT* logger's level (not global) will block messages
        //   (assuming using JDK, not Log4J)
        Logger.getLogger("").setLevel(Level.WARNING); // empty name = root logger
    }

    static private <T> T getResponse(HttpURLConnection clientConnection, Class<T> customClass) throws IOException {
        Moshi moshi = new Moshi.Builder().build();
        return moshi.adapter(customClass).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
    }

    /**
     * Shared state for all tests. We need to be able to mutate it (adding recipes etc.) but never need to replace
     * the reference itself. We clear this state out after every test runs.
     */

    final Set<List<List<String>>> storage = new HashSet<>();

    @BeforeEach
    public void setup() {
        // Re-initialize state, etc. for _every_ test method run
        storage.clear();
        this.cache = new WeatherCachingProxy(30, 20, TimeUnit.MINUTES, 0.7);

        // In fact, restart the entire Spark server for every test!
        Spark.get("/loadcsv", new LoadHandler(storage));
        Spark.get("viewcsv", new ViewHandler(storage));
        Spark.get("/searchcsv", new SearchHandler(storage));
        Spark.get("/weather", new WeatherHandler());
        Spark.init();
        Spark.awaitInitialization(); // don't continue until the server is listening
    }

    @AfterEach
    public void teardown() {
        // Gracefully stop Spark listening on both endpoints
        Spark.unmap("/loadcsv");
        Spark.unmap("/viewcsv");
        Spark.unmap("/searchcsv");
        Spark.unmap("/weather");
        Spark.awaitStop(); // don't proceed until the server is stopped
    }

    /**
     * Helper to start a connection to a specific API endpoint/params
     *
     * @param apiCall the call string, including endpoint
     *                (NOTE: this would be better if it had more structure!)
     * @return the connection for the given URL, just after connecting
     * @throws IOException if the connection fails for some reason
     */
    static private HttpURLConnection tryRequest(String apiCall) throws IOException {
        // Configure the connection (but don't actually send the request yet)
        URL requestURL = new URL("http://localhost:" + Spark.port() + "/" + apiCall);
        HttpURLConnection clientConnection = (HttpURLConnection) requestURL.openConnection();

        // The default method is "GET", which is what we're using here.
        // If we were using "POST", we'd need to say so.
        //clientConnection.setRequestMethod("GET");

        clientConnection.connect();
        return clientConnection;
    }
    @Test
    public void testAPIWeatherProv() throws IOException {
        HttpURLConnection clientConnection = tryRequest("weather?lat=41.8268&lon=-71.4029");
        assertEquals(200, clientConnection.getResponseCode());
        WeatherHandler.WeatherSuccessResponse response = getResponse(clientConnection, WeatherHandler.WeatherSuccessResponse.class);

        assertNotNull(response);
        assertEquals("success", response.result());

        clientConnection.disconnect();
    }

    @Test
    public void testAPIWeatherLongCoords() throws IOException {
        HttpURLConnection clientConnection = tryRequest("weather?lat=41.82683284239819824838175&lon=-71.4029123721899462876419807");
        assertEquals(200, clientConnection.getResponseCode());
        WeatherHandler.WeatherSuccessResponse response = getResponse(clientConnection, WeatherHandler.WeatherSuccessResponse.class);

        assertNotNull(response);
        assertEquals("success", response.result());

        clientConnection.disconnect();
    }

    @Test
    public void testAPIWeatherInvalidCoords() throws IOException {
        HttpURLConnection clientConnection = tryRequest("weather?lat=1000&lon=-1000");
        assertEquals(200, clientConnection.getResponseCode());
        WeatherHandler.WeatherFailureResponse response = getResponse(clientConnection, WeatherHandler.WeatherFailureResponse.class);

        assertNotNull(response);
        assertEquals("error_internal", response.result());
        assertEquals("Unexpected error. This will be logged internally and investigated.", response.message());

        clientConnection.disconnect();
    }

    @Test
    public void testAPIWeatherNoLat() throws IOException {
        HttpURLConnection clientConnection = tryRequest("weather?lon=-71.4029");
        assertEquals(200, clientConnection.getResponseCode());
        WeatherHandler.WeatherFailureResponse response = getResponse(clientConnection, WeatherHandler.WeatherFailureResponse.class);

        assertNotNull(response);
        assertEquals("error_bad_request", response.result());
        assertEquals("Missing \"lon\" or \"lat\" field.", response.message());

        clientConnection.disconnect();
    }

    @Test
    public void testAPIWeatherNoLon() throws IOException {
        HttpURLConnection clientConnection = tryRequest("weather?lat=41.8268");
        assertEquals(200, clientConnection.getResponseCode());
        WeatherHandler.WeatherFailureResponse response = getResponse(clientConnection, WeatherHandler.WeatherFailureResponse.class);

        assertNotNull(response);
        assertEquals("error_bad_request", response.result());
        assertEquals("Missing \"lon\" or \"lat\" field.", response.message());

        clientConnection.disconnect();
    }

    @Test
    public void testAPIWeatherNotNumbers() throws IOException {
        HttpURLConnection clientConnection = tryRequest("weather?lat=yuh&lon=yuh");
        assertEquals(200, clientConnection.getResponseCode());
        WeatherHandler.WeatherFailureResponse response = getResponse(clientConnection, WeatherHandler.WeatherFailureResponse.class);

        assertNotNull(response);
        assertEquals("error_bad_request", response.result());
        assertEquals("Failed to parse number values from the request point.", response.message());

        clientConnection.disconnect();
    }
}