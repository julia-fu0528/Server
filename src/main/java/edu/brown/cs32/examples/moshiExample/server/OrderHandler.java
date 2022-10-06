package edu.brown.cs32.examples.moshiExample.server;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import edu.brown.cs32.examples.moshiExample.ingredients.Carrots;
import edu.brown.cs32.examples.moshiExample.ingredients.HotPeppers;
import edu.brown.cs32.examples.moshiExample.ingredients.Ingredient;
import edu.brown.cs32.examples.moshiExample.soup.Soup;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;
import java.util.Set;

/**
 * Handler class for the soup ordering API endpoint.
 *
 * This endpoint is similar to the endpoint(s) you'll need to create for Sprint 2. It takes a basic GET request with
 * no Json body, and returns a Json object in reply. The responses are more complex, but this should serve as a reference.
 *
 */
public class OrderHandler implements Route {
    private final Set<Soup> menu;

    /**
     * Constructor accepts some shared state
     * @param menu the shared state (note: we *DON'T* want to make a defensive copy here!
     */
    public OrderHandler(Set<Soup> menu) {
        this.menu = menu;
    }

    /**
     * Pick a convenient soup and make it. the most "convenient" soup is the first recipe we find in the unordered
     * set of recipe cards.
     * @param request the request to handle
     * @param response use to modify properties of the response
     * @return response content
     * @throws Exception This is part of the interface; we don't have to throw anything.
     */
    @Override
    public Object handle(Request request, Response response) throws Exception {
        for(Soup soup : menu) {
            // Just make the first one
            return new SoupSuccessResponse(soup.ingredients()).serialize();
        }
        return new SoupNoRecipesFailureResponse().serialize();

        // NOTE: beware this "return Object" and "throws Exception" idiom. We need to follow it because
        //   the library uses it, but in general this is lowers the protection of the type system.
    }

    /**
     * Response object to send, containing a soup with certain ingredients in it
     */
    public record SoupSuccessResponse(String response_type, Set<Ingredient> ingredients) {
        public SoupSuccessResponse(Set<Ingredient> ingredients) {
            this("success", ingredients);
        }
        /**
         * @return this response, serialized as Json
         */
        String serialize() {
            try {
                // Just like in SoupAPIUtilities.
                //   (How could we rearrange these similar methods better?)
                Moshi moshi = new Moshi.Builder()
                        .add(
                                // Expect something that's an Ingredient...
                                PolymorphicJsonAdapterFactory.of(Ingredient.class, "type")
                                        // ...with two possibilities for concrete shapes, disambiguated by type:
                                        .withSubtype(Carrots.class, "carrot")
                                        .withSubtype(HotPeppers.class, "hotpeppers")
                        )
                        .build();
                JsonAdapter<SoupSuccessResponse> adapter = moshi.adapter(SoupSuccessResponse.class);
                return adapter.toJson(this);
            } catch(Exception e) {
                // For debugging purposes, show in the console _why_ this fails
                // Otherwise we'll just get an error 500 from the API in integration
                // testing.
                e.printStackTrace();
                throw e;
            }
        }
    }

    /**
     * Response object to send if someone requested soup before any recipes were loaded
     */
    public record SoupNoRecipesFailureResponse(String response_type) {
        public SoupNoRecipesFailureResponse() { this("error"); }

        /**
         * @return this response, serialized as Json
         */
        String serialize() {
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(SoupNoRecipesFailureResponse.class).toJson(this);
        }
    }

}
