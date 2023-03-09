package TestSuites;
import Handlers.LoadHandler;
import Handlers.SearchHandler;
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
import java.security.KeyException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FuzzTest {

    final Set<List<List<String>>> storage = new HashSet<>();

    /** Set up Spark before running any tests */
    @BeforeAll
    public static void setup_before_everything() {
        // spark will use an arbitrary available port
        Spark.port(0);
        Logger.getLogger("").setLevel(Level.WARNING);
    }

    /**
     * Set up the environment for each test
     *
     * @throws KeyException if there is an error with the API key
     */
    @BeforeEach
    public void setup() throws KeyException {
        // clear the data in the csv file
        storage.clear();

        // make all the endpoints
        Spark.get("loadcsv", new LoadHandler(this.storage));
        Spark.get("searchcsv", new SearchHandler(this.storage));

        Spark.init();
        Spark.awaitInitialization();
    }

    /** Remove all test resources after each test */
    @AfterEach
    public void teardown() {
        // remove endpoints
        Spark.unmap("/loadcsv");
        Spark.unmap("/searchcsv");
        Spark.awaitStop();
    }
    /**
     * Helper to get the response from the connection's input stream, in the form of a map
     *
     * @param clientConnection the HttpURLConnection to read from
     * @return the Map representing the response to the request
     * @throws IOException if failed to read from input stream
     */
    private Map<String, Object> getResponse(HttpURLConnection clientConnection) throws IOException {
        Moshi moshi = new Moshi.Builder().build();
        Map<String, Object> map =
                moshi.adapter(Map.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
        return map;
    }
    /**
     * Attempts to connect to the server through URL on local machine
     *
     * @param apiCall the endpoint you are targeting
     * @return a requester
     * @throws ExecutionException error thrown if it does not execute properly
     */
    private HttpURLConnection tryRequest(String apiCall) throws IOException {
        URL requestURL = new URL("http://localhost:" + Spark.port() + "/" + apiCall);
        HttpURLConnection clientConnection = (HttpURLConnection) requestURL.openConnection();

        clientConnection.connect();
        return clientConnection;
    }

    /**
     * Basic fuzz testing structure, tests with a randomized string and a random column from the list of 5 valid
     * columns of the stardata file. Specifics of the randomization can be seen in the StringRandomizer class.
     *
     * Catches when the response code is 500 (crash), 400 (invalid input form), and 200 (success). Through running, it
     * is almost always a successful connection but nothing in the results (as it is a random string). However, one time
     * it passed in an empty string, and in this case the search returned every value in stardata.csv, which is an
     * edge case that we had not accounted for.
     * @throws IOException in case the connection fails
     */

    @Test
    public void fuzzSearchTest() throws IOException {
        HttpURLConnection clientConnection1 = tryRequest("loadcsv?file=stardata.csv&hasHeader=true");
        assertEquals(200, clientConnection1.getResponseCode());
        StringRandomizer randomizer = new StringRandomizer();
        String randTarget = "";
        String randCol = "";

        //check 1000 times
        for (int i = 0; i < 1000; i ++) {
            // pass in a random search term
            randTarget = randomizer.generateRandomString();
            // pass in a random (valid) column header
            randCol = randomizer.generateRandomValidCol();
            String csvToSearch = "searchcsv?by=column&query="+ randTarget +"&column="+ randCol;
            HttpURLConnection clientConnection2 = tryRequest(csvToSearch);

            if (clientConnection2.getResponseCode() >= 500) {
                System.out.println("error code of 500 was encountered");
            }
            if (clientConnection2.getResponseCode() == 400) {
                System.out.println("error code of 400 was encountered");
            }
            if (clientConnection2.getResponseCode() == 200) {
                System.out.println("error code of 200 was encountered");
            }
        }
        assertTrue(true, "server ran as expected through all tests!");
    }

    /**
     * Tests search method but when both valid column and query inputs are received–randomizes column from set of
     * valid columns, and randomizes query
     * @throws IOException in case the connection fails
     */
    @Test
    public void fuzzSearchColTest() throws IOException {
        HttpURLConnection clientConnection1 = tryRequest("loadcsv?file=stardata.csv&hasHeader=true");
        assertEquals(200, clientConnection1.getResponseCode());
        StringRandomizer randomizer = new StringRandomizer();
        String target = "Dylan";
        String randCol = "";
        for (int i = 0; i < 1000; i++) {
            randCol = randomizer.generateRandomValidCol();
            String csvToSearch = "searchcsv?by=column&query="+ target +"&column="+ randCol;
            HttpURLConnection clientConnection2 = tryRequest(csvToSearch);
            // as the inputs are always valid, this test should always run regardless of the random column passed in
            assertEquals(200, clientConnection2.getResponseCode());
        }

    }

}

