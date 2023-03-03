package TestSuites;

import Handlers.LoadHandler;
import Handlers.SearchHandler;
import Handlers.ViewHandler;
import MockedData.MockedCSV;
import com.squareup.moshi.Moshi;
import Servers.LoadedFiles;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testing Plan:
 * (A). Loadcsv
 * 1. MissingFilePathResponse: missing filepath query
 * 2. InaccessibleCSVResponse: filepath doesn't exist
 * 3. CSVParsingSyccessResponse: empty file
 * 4. CSVParsingSuccessResponse: basic file
 *
 * (B). Viewcsv
 * 1. ViewCSVFailureResponse: No csv stored yet
 * 2. ViewCSVSuccessResponse: empty csv
 * 3. ViewCSVSuccessResponse: empty csv with headers
 * 4. ViewCSVSuccessResponse: small csv
 * 5. ViewCSVSuccessResponse: large csv
 *
 * (C). Searchcsv
 * 1. MissingColumnResponse: no column query
 * 2. MissingValueResponse: no value query
 * 3. SearchFailureResponse: no value found
 * 4. SearchSuccessResponse: column index
 * 5. SearchSuccessResponse: column header
 * 6. SearchSuccessResponse: single row
 * 7. SearchSuccessResponse: multiple rows
 */
public class TestLoad {

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

    /**
     * Shared state for all tests. We need to be able to mutate it (adding recipes etc.) but never need to replace
     * the reference itself. We clear this state out after every test runs.
     */

    final Set<List<List<String>>> storage = new HashSet<>();

    @BeforeEach
    public void setup() {
        // Re-initialize state, etc. for _every_ test method run
        storage.clear();

        // In fact, restart the entire Spark server for every test!
        Spark.get("/loadcsv", new LoadHandler(new LoadedFiles<List<List<String>>>()));
        Spark.get("viewcsv", new ViewHandler());
        Spark.get("/searchcsv", new SearchHandler());
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
     * @param apiCall the call string, including endpoint
     *                (NOTE: this would be better if it had more structure!)
     * @return the connection for the given URL, just after connecting
     * @throws IOException if the connection fails for some reason
     */
    static private HttpURLConnection tryRequest(String apiCall) throws IOException {
        // Configure the connection (but don't actually send the request yet)
        URL requestURL = new URL("http://localhost:"+Spark.port()+"/"+apiCall);
        HttpURLConnection clientConnection = (HttpURLConnection) requestURL.openConnection();

        // The default method is "GET", which is what we're using here.
        // If we were using "POST", we'd need to say so.
        //clientConnection.setRequestMethod("GET");

        clientConnection.connect();
        return clientConnection;
    }
    @Test
    // Recall that the "throws IOException" doesn't signify anything but acknowledgement to the type checker
    public void testLoadMissingPath() throws IOException {
        HttpURLConnection clientConnection = tryRequest("loadcsv");
        // Get an OK response (the *connection* worked, the *API* provides an error response)
        assertEquals(200, clientConnection.getResponseCode());

        // Now we need to see whether we've got the expected Json response.
        // SoupAPIUtilities handles ingredient lists, but that's not what we've got here.
        Moshi moshi = new Moshi.Builder().build();
        // We'll use okio's Buffer class here
        LoadHandler.MissingFilePathResponse response =
                moshi.adapter(LoadHandler.MissingFilePathResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

        // ^ If that succeeds, we got the expected response. Notice that this is *NOT* an exception, but a real Json reply.
        assertEquals("error_bad_request",
                response.result());
        assertEquals("Missing filepath query.",
                response.message());
        clientConnection.disconnect();
    }
    @Test
    public void testLoadInvalidCSV() throws IOException {
        HttpURLConnection clientConnection = tryRequest(MockedCSV.invalid_query);
        assertEquals(200, clientConnection.getResponseCode());

        Moshi moshi = new Moshi.Builder().build();
        LoadHandler.InaccessibleCSVResponse response =
                moshi.adapter(LoadHandler.InaccessibleCSVResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
        assertEquals("error_datasource",
                response.result());
        assertEquals(MockedCSV.invalid_filepath,
                response.filepath());
        assertEquals("File '" + MockedCSV.invalid_filepath + "'doesn't exist. ",
                response.message());
        clientConnection.disconnect();
    }
    @Test
    public void testLoadSuccessEmpty() throws IOException {
        HttpURLConnection clientConnection = tryRequest(MockedCSV.emptycsv_query);
        assertEquals(200, clientConnection.getResponseCode());

        Moshi moshi = new Moshi.Builder().build();
        LoadHandler.CSVParsingSuccessResponse response =
                moshi.adapter(LoadHandler.CSVParsingSuccessResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
        assertEquals("success",
                response.result());
        assertEquals(MockedCSV.emptycsv_path,
                response.filepath());
        assertEquals("CSV File'" + MockedCSV.emptycsv_path + "' successfully stored. " +
                        "Contents accessible in endpoint viewcsv",
                response.message());
        clientConnection.disconnect();
    }
    @Test
    public void testLoadSuccessNonempty() throws IOException {
        HttpURLConnection clientConnection = tryRequest(MockedCSV.stardata_query);
        assertEquals(200, clientConnection.getResponseCode());

        Moshi moshi = new Moshi.Builder().build();
        LoadHandler.CSVParsingSuccessResponse response =
                moshi.adapter(LoadHandler.CSVParsingSuccessResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
        assertEquals("success",
                response.result());
        assertEquals(MockedCSV.stardata_path,
                response.filepath());
        assertEquals("CSV File'" + MockedCSV.stardata_path + "' successfully stored. " +
                        "Contents accessible in endpoint viewcsv",
                response.message());
        clientConnection.disconnect();
    }

//    @Test
//    // Recall that the "throws IOException" doesn't signify anything but acknowledgement to the type checker
//    public void testAPIOneRecipe() throws IOException {
//
////        menu.add(Soup.buildNoExceptions(true, Set.of(
////                new Carrots(Carrots.CarrotChopType.MATCHSTICK, 6.0),
////                new HotPeppers(1, 2.0))));
//
//        HttpURLConnection clientConnection = tryRequest("loadcsv");
//        // Get an OK response (the *connection* worked, the *API* provides an error response)
//        assertEquals(200, clientConnection.getResponseCode());
//
//        // Now we need to see whether we've got the expected Json response.
//        // SoupAPIUtilities handles ingredient lists, but that's not what we've got here.
//        // NOTE:   (How could we reduce the code repetition?)
//        Moshi moshi = new Moshi.Builder().build();
//        // NOTE: We're using a lot of raw strings here. What could we do about that?
//
//        // We'll use okio's Buffer class here
//        LoadHandler response =
//                moshi.adapter(LoadHandler.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
//
//        // ^ If that succeeds, we got the expected response. But we should also check the ingredients
//        assertEquals(Set.of(
//                new Carrots(Carrots.CarrotChopType.MATCHSTICK, 6.0),
//                new HotPeppers(1, 2.0)),
//                    response.ingredients());
//
//        clientConnection.disconnect();
//    }
}
