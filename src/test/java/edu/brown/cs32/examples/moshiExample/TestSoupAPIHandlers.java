package edu.brown.cs32.examples.moshiExample;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import edu.brown.cs32.examples.moshiExample.ingredients.Carrots;
import edu.brown.cs32.examples.moshiExample.ingredients.HotPeppers;
import edu.brown.cs32.examples.moshiExample.ingredients.Ingredient;
import edu.brown.cs32.examples.moshiExample.server.OrderHandler;
import edu.brown.cs32.examples.moshiExample.soup.Soup;
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the actual handlers.
 *
 * On Sprint 2, you'll need to deserialize API responses. Although this demo doesn't need to do that,
 * these _tests_ do.
 *
 * https://junit.org/junit5/docs/current/user-guide/
 *
 * Because these tests exercise more than one "unit" of code, they're not "unit tests"...
 *
 * If the backend were "the system", we might call these system tests. But
 * I prefer "integration test" since, locally, we're testing how the Soup
 * functionality connects to the handler. These distinctions are sometimes
 * fuzzy and always debatable; the important thing is that these ARE NOT
 * the usual sort of unit tests.
 *
 * Note: if we were doing this for real, we might want to test encoding formats
 * other than UTF-8 (StandardCharsets.UTF_8).
 */
public class TestSoupAPIHandlers {

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

    final Set<Soup> menu = new HashSet<>();

    @BeforeEach
    public void setup() {
        // Re-initialize state, etc. for _every_ test method run
        menu.clear();

        // In fact, restart the entire Spark server for every test!
        Spark.get("/order", new OrderHandler(menu));
        Spark.init();
        Spark.awaitInitialization(); // don't continue until the server is listening
    }

    @AfterEach
    public void teardown() {
        // Gracefully stop Spark listening on both endpoints
        Spark.unmap("/order");
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
    public void testAPINoRecipes() throws IOException {
        HttpURLConnection clientConnection = tryRequest("order");
        // Get an OK response (the *connection* worked, the *API* provides an error response)
        assertEquals(200, clientConnection.getResponseCode());

        // Now we need to see whether we've got the expected Json response.
        // SoupAPIUtilities handles ingredient lists, but that's not what we've got here.
        Moshi moshi = new Moshi.Builder().build();
        // We'll use okio's Buffer class here
        OrderHandler.SoupNoRecipesFailureResponse response =
                moshi.adapter(OrderHandler.SoupNoRecipesFailureResponse.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

        // ^ If that succeeds, we got the expected response. Notice that this is *NOT* an exception, but a real Json reply.

        clientConnection.disconnect();
    }

    @Test
    // Recall that the "throws IOException" doesn't signify anything but acknowledgement to the type checker
    public void testAPIOneRecipe() throws IOException {

        menu.add(Soup.buildNoExceptions(true, Set.of(
                new Carrots(Carrots.CarrotChopType.MATCHSTICK, 6.0),
                new HotPeppers(1, 2.0))));

        HttpURLConnection clientConnection = tryRequest("order");
        // Get an OK response (the *connection* worked, the *API* provides an error response)
        assertEquals(200, clientConnection.getResponseCode());

        // Now we need to see whether we've got the expected Json response.
        // SoupAPIUtilities handles ingredient lists, but that's not what we've got here.
        // NOTE:   (How could we reduce the code repetition?)
        Moshi moshi = new Moshi.Builder().add(
                // Expect something that's an Ingredient...
                PolymorphicJsonAdapterFactory.of(Ingredient.class, "type")
                        // ...with two possibilities for concrete shapes, disambiguated by type:
                        .withSubtype(Carrots.class, "carrot")
                        .withSubtype(HotPeppers.class, "hotpeppers"))
                .build();
        // NOTE: We're using a lot of raw strings here. What could we do about that?

        // We'll use okio's Buffer class here
        OrderHandler.SoupSuccessResponse response =
                moshi.adapter(OrderHandler.SoupSuccessResponse.class).fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

        // ^ If that succeeds, we got the expected response. But we should also check the ingredients
        assertEquals(Set.of(
                new Carrots(Carrots.CarrotChopType.MATCHSTICK, 6.0),
                new HotPeppers(1, 2.0)),
                    response.ingredients());

        clientConnection.disconnect();
    }
}
