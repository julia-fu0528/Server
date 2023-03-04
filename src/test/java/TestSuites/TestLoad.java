package TestSuites;

import CSV.Algos.CSVParser;
import CSV.Algos.Search;
import CSV.RowCreators.RowCreator.ListCreator;
import Exceptions.SearchFailureException;
import Handlers.LoadHandler;
import Handlers.SearchHandler;
import Handlers.ViewHandler;
import Weather.Requester.PlainRequester;
import Weather.WeatherCachingProxy;
import Weather.WeatherHandler;
import com.squareup.moshi.Moshi;
import Servers.LoadedFiles;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Spark;

import java.io.FileNotFoundException;
import java.io.FileReader;
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

        Spark.port(0);
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
        Spark.init();
        Spark.get("/loadcsv", new LoadHandler(storage));
        Spark.get("viewcsv", new ViewHandler(storage));
        Spark.get("/searchcsv", new SearchHandler(storage));
        // Spark.init();
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
        HttpURLConnection clientConnection = tryRequest("loadcsv?filepath=starsdatas.csv");
        assertEquals(200, clientConnection.getResponseCode());

        Moshi moshi = new Moshi.Builder().build();
        LoadHandler.InaccessibleCSVResponse response =
                moshi.adapter(LoadHandler.InaccessibleCSVResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
        assertEquals("error_datasource",
                response.result());
        assertEquals("starsdatas.csv",
                response.filepath());
        assertEquals("File '" + "starsdatas.csv" + "'doesn't exist. ",
                response.message());
        clientConnection.disconnect();
    }
    @Test
    public void testSuccessEmpty() throws IOException {
        // tests loadcsv for empty file
        String emptycsv_query = "loadcsv?filepath=src/main/data/made-example-files/empty.csv";
        HttpURLConnection clientConnection = tryRequest(emptycsv_query);
        assertEquals(200, clientConnection.getResponseCode());

        String emptycsv_path = "src/main/data/made-example-files/empty.csv";
        Moshi moshi = new Moshi.Builder().build();
        LoadHandler.CSVParsingSuccessResponse response =
                moshi.adapter(LoadHandler.CSVParsingSuccessResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
        assertEquals("success",
                response.result());
        assertEquals(emptycsv_path,
                response.filepath());
        assertEquals("CSV File'" + emptycsv_path + "' successfully stored. " +
                        "Contents accessible in endpoint viewcsv",
                response.message());

        clientConnection.disconnect();
    }
    @Test
    public void testSuccessNonempty() throws IOException {
        // test load csv for nonempty file
        String stardata_query = "loadcsv?filepath=src/main/data/stars/stardata.csv";
        String stardata_path = "src/main/data/stars/stardata.csv";
        HttpURLConnection clientConnection = tryRequest(stardata_query);
        assertEquals(200, clientConnection.getResponseCode());

        Moshi moshi = new Moshi.Builder().build();
        LoadHandler.CSVParsingSuccessResponse response =
                moshi.adapter(LoadHandler.CSVParsingSuccessResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
        assertEquals("success",
                response.result());
        assertEquals(stardata_path,
                response.filepath());
        assertEquals("CSV File'" + stardata_path + "' successfully stored. " +
                        "Contents accessible in endpoint viewcsv",
                response.message());


        clientConnection.disconnect();
    }
}
