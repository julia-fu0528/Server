package TestSuites;

import CSV.Algos.CSVParser;
import CSV.Algos.Search;
import CSV.RowCreators.RowCreator.ListCreator;
import Exceptions.SearchFailureException;
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
public class TestSearch {
    public TestSearch() throws FileNotFoundException {
    }

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
    String headers_empty_query = "loadcsv?filepath=src/main/data/made-example-files/empty-with-headers.csv";
    String headers_empty_path = "src/main/data/made-example-files/empty-with-headers.csv";
    List<List<String>> headers_empty_file= new CSVParser(new FileReader(headers_empty_path), new ListCreator()).parse();
    String noheaders_people_query = "loadcsv?filepath=src/main/data/made-example-files/people-has-header.csv";
    String noheaders_people_path = "src/main/data/made-example-files/people-has-header.csv";
    List<List<String>> noheaders_people_file = new CSVParser(new FileReader(noheaders_people_path), new ListCreator()).parse();
    String stardata_query = "loadcsv?filepath=src/main/data/stars/stardata.csv";
    String stardata_path = "src/main/data/stars/stardata.csv";
    List<List<String>> stardata_file = new CSVParser(new FileReader(stardata_path), new ListCreator()).parse();
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
    public void testSearchNoColumn() throws IOException {
        HttpURLConnection clientConnection = tryRequest("searchcsv");
        assertEquals(200, clientConnection.getResponseCode());

        Moshi moshi = new Moshi.Builder().build();
        SearchHandler.MissingColumnResponse response =
                moshi.adapter(SearchHandler.MissingColumnResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

        assertEquals("error_bad_request",
                response.result());
        assertEquals("Missing column query",
                response.message());
        clientConnection.disconnect();
    }
    @Test
    public void testSearchNoValue() throws IOException {
        HttpURLConnection clientConnection = tryRequest("searchcsv?column=\'Name\'");
        assertEquals(200, clientConnection.getResponseCode());

        Moshi moshi = new Moshi.Builder().build();
        SearchHandler.MissingValueResponse response =
                moshi.adapter(SearchHandler.MissingValueResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

        assertEquals("error_bad_request",
                response.result());
        assertEquals("Missing value query",
                response.message());
        clientConnection.disconnect();
    }
    @Test
    public void testValueNotFound() throws IOException {
        this.storage.add(headers_empty_file);
        HttpURLConnection clientConnection = tryRequest(headers_empty_query);
        assertEquals(200, clientConnection.getResponseCode());

        // tests search csv for nonempty file, value not found, search column header
        HttpURLConnection clientConnection_search = tryRequest("searchcsv?column=First+Name&value=Rory");
        assertEquals(200, clientConnection_search.getResponseCode());
        Moshi moshi_search = new Moshi.Builder().build();
        SearchHandler.ValueNotFoundResponse response_search =
                moshi_search.adapter(SearchHandler.ValueNotFoundResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection_search.getInputStream()));
        assertEquals("error.json",
                response_search.result());
        assertEquals("First Name",
                response_search.column());
        assertEquals("Rory",
                response_search.value());
        assertEquals("The value ' Rory' can't be found at column First Name",
                response_search.message());

        clientConnection.disconnect();
    }
    @Test
    public void testSuccessMultipleRows() throws IOException {
        this.storage.add(noheaders_people_file);
        HttpURLConnection clientConnection = tryRequest(noheaders_people_query);
        assertEquals(200, clientConnection.getResponseCode());

        List<List<String>> people_searched;
        try {
            people_searched = new Search(noheaders_people_file).searchTarget("3", "student");
        } catch (SearchFailureException e) {
            throw new RuntimeException(e);
        }

        // tests search csv for nonempty file, search column index, value present multiple times
        HttpURLConnection clientConnection_search = tryRequest("searchcsv?column=3&value=student");
        assertEquals(200, clientConnection_search.getResponseCode());
        Moshi moshi_search = new Moshi.Builder().build();
        SearchHandler.SearchSuccessResponse response_search =
                moshi_search.adapter(SearchHandler.SearchSuccessResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection_search.getInputStream()));
        assertEquals("success",
                response_search.result());
        assertEquals(people_searched,
                response_search.data());
        assertEquals("3",
                response_search.column());
        assertEquals("student",
                response_search.value());
        assertEquals("Value successfully searched",
                response_search.message());

        clientConnection.disconnect();
    }
    @Test
    public void testSuccessNonempty() throws IOException {
        this.storage.add(stardata_file);
        // test load csv for nonempty file
        HttpURLConnection clientConnection = tryRequest(stardata_query);
        assertEquals(200, clientConnection.getResponseCode());


        List<List<String>> star_searched;
        try {
            star_searched = new Search(stardata_file).searchTarget("ProperName", "Rory");
        } catch (SearchFailureException e) {
            throw new RuntimeException(e);
        }

        // tests search csv for nonempty file, search column header, value present once
        HttpURLConnection clientConnection_search = tryRequest("searchcsv?column=ProperName&value=Rory");
        assertEquals(200, clientConnection_search.getResponseCode());
        Moshi moshi_search = new Moshi.Builder().build();
        SearchHandler.SearchSuccessResponse response_search =
                moshi_search.adapter(SearchHandler.SearchSuccessResponse.class).
                        fromJson(new Buffer().readFrom(clientConnection_search.getInputStream()));
        assertEquals("success",
                response_search.result());
        assertEquals(star_searched,
                response_search.data());
        assertEquals("ProperName",
                response_search.column());
        assertEquals("Rory",
                response_search.value());
        assertEquals("Value successfully searched",
                response_search.message());

        clientConnection.disconnect();
    }
}
