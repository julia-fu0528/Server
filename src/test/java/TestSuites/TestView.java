package TestSuites;

import Handlers.LoadHandler;
import Handlers.SearchHandler;
import Handlers.ViewHandler;
import Servers.LoadedFiles;
import Weather.Requester.PlainRequester;
import Weather.WeatherCachingProxy;
import Weather.WeatherHandler;
import com.squareup.moshi.Moshi;
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
public class TestView {
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
        Spark.get("/loadcsv", new LoadHandler(storage));
        Spark.get("viewcsv", new ViewHandler(storage));
        Spark.get("/searchcsv", new SearchHandler(storage));
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
    public void testViewUnstored() throws IOException {
        HttpURLConnection clientConnection = tryRequest("viewcsv");
        assertEquals(200, clientConnection.getResponseCode());

        Moshi moshi = new Moshi.Builder().build();
        ViewHandler.ViewCSVFailureResponse response =
                moshi.adapter(ViewHandler.ViewCSVFailureResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

        assertEquals("error_datasource",
                response.result());
        assertEquals("No CSV file stored yet.",
                response.message());
        clientConnection.disconnect();
    }
//    @Test
//    public void testViewSuccessEmpty() throws IOException {
//        HttpURLConnection clientConnection = tryRequest("viewcsv");
//        assertEquals(200, clientConnection.getResponseCode());
//
//        // ViewHandler(new LoadedFiles().storeFile(new CSVParser(new FileReader(MockedCSV.emptycsv_path), new ListCreator()).parse()));
//        Moshi moshi = new Moshi.Builder().build();
//        ViewHandler.ViewCSVFailureResponse response =
//                moshi.adapter(ViewHandler.ViewCSVFailureResponse.class).
//                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
//
//        assertEquals("error_datasource",
//                response.result());
//        assertEquals("No CSV file stored yet.",
//                response.message());
//        clientConnection.disconnect();
//    }
}
