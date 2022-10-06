package edu.brown.cs32.examples.moshiExample;

import edu.brown.cs32.examples.moshiExample.ingredients.Carrots;
import edu.brown.cs32.examples.moshiExample.ingredients.HotPeppers;
import edu.brown.cs32.examples.moshiExample.server.SoupAPIUtilities;
import edu.brown.cs32.examples.moshiExample.soup.ActualFlavorException;
import edu.brown.cs32.examples.moshiExample.soup.Soup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *  Test deserializing soup recipes
 *
 *  Because we're using JUnit here, we needed to add JUnit to pom.xml.
 *
 *  In a real application, we'd want to test better---e.g., if it's part of our spec that SoupHandler throws
 *  an IOException on invalid JSON, we'd want to test that.
 */
public class TestSoupAPIUtilities {

    @BeforeEach
    public void setup() {
        // No setup
    }

    @AfterEach
    public void teardown() {
        // No setup
    }

    @Test
    public void testFrom_ValidIngredientsList() throws IOException, ActualFlavorException {
        // Without special processing, JSON strings must use double quotes.
        String mild = "[{\"type\": \"carrot\", \"amount\": 5}, " +
                       "{\"type\": \"hotpeppers\", \"amount\": 1, \"scovilles\": 100}]";

        // This might throw an IOException, but if so JUnit will mark the test as failed.
        Soup soup = SoupAPIUtilities.fromJSON(mild, true);

        assertEquals(2, soup.ingredients().size());
    }

    @Test
    public void testFrom_TooSpicyIngredientsList() {
        // Without special processing, JSON strings must use double quotes.
        String hot = "[{\"type\": \"carrot\", \"amount\": 5}, {\"type\": \"hotpeppers\", \"amount\": 1, \"scovilles\": 90000}]";

        // This *should* throw a flavor exception. Don't forget to wrap the offending statement in a function,
        // or JUnit won't be able to detect the exception properly.
        assertThrows(ActualFlavorException.class,
                () -> SoupAPIUtilities.fromJSON(hot, true));
    }

    /**
     * This saves some effort vs. repeating the code as in edu.brown.cs32.examples.moshiExample.TestSoupAPIHandlers.
     * @return a freshly created Soup object containing matchstick carrots and very mild peppers.
     */
    static Soup mixFreshBowlTimid() {
        return Soup.buildNoExceptions(true, Set.of(
                new Carrots(Carrots.CarrotChopType.MATCHSTICK, 6.0),
                new HotPeppers(1, 2.0)));
    }

    @Test
    public void testTo_ValidSoup() throws ActualFlavorException, IOException {
        Soup bowl = mixFreshBowlTimid();
        String json = SoupAPIUtilities.toJson(bowl);
        //System.out.println(json);
        // Don't try to parse the string yourself to test it.
        // Instead, use a Json library to look at the info provided.
        Soup result = SoupAPIUtilities.fromJSON(json, true);
        // This will FAIL if we don't define equals in Soup
        assertEquals(bowl, result);
        // If the above produces an exception, the JUnit test will fail.
    }

}
