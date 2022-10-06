package edu.brown.cs32.examples.moshiExample.server;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import edu.brown.cs32.examples.moshiExample.ingredients.Carrots;
import edu.brown.cs32.examples.moshiExample.ingredients.HotPeppers;
import edu.brown.cs32.examples.moshiExample.ingredients.Ingredient;
import edu.brown.cs32.examples.moshiExample.soup.ActualFlavorException;
import edu.brown.cs32.examples.moshiExample.soup.Soup;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * This class contains utility methods for handling soup objects and Json-encoded soup objects.
 * Primarily this means _serializing_ Soup objects to Json and
 *                      _deserializing_ Soup objects from Json
 *
 * Use this as a reference for polymorphic serialization/deserialization. It
 * shouldn't be necessary on Sprint 2 functionality, and Sprint 2 testing
 * only requires Maps for response types.
 */
public class SoupAPIUtilities {

    /**
     * This class isn't meant to be instantiated for this demo: it's just got one static method in it.
     */
    private SoupAPIUtilities() {}

    /**
     * Mixes up a fresh bowl of soup, based on a JSON object that says what ingredients are in it. Yum!
     * The recipe book doesn't say whether or not the chef is timid, and so this method needs to be told
     * before it can actually make the soup.
     *
     * @param jsonList the ingredients as a JSON list
     * @param timidChef whether or not the chef is worried about making the soup too spicy
     * @throws JsonDataException if the given JSON doesn't conform to the soup specification
     *   (we're re-using Moshi's JsonDataException here; this may or may not be a good choice in reality)
     * @throws IOException if the given string isn't valid JSON
     * @return the Soup object
     */
    public static Soup fromJSON(String jsonList, boolean timidChef) throws JsonDataException, IOException, ActualFlavorException {
        // Create an adapter to read the json string (hopefully) into Ingredient objects
        // There's one challenge: as the writer of this class, we don't know *which* ingredients
        // will arrive. Fortunately, Moshi lets us disambiguate between them...
        //   (Technical note: this is what we need the 2nd Moshi dependency for; Polymorphic adapters
        //   aren't in the basic package.)
        Moshi moshi = new Moshi.Builder()
                .add(
                        // Expect something that's an Ingredient...
                        PolymorphicJsonAdapterFactory.of(Ingredient.class, "type")
                                // ...with two possibilities for concrete shapes, disambiguated by type:
                                .withSubtype(Carrots.class, "carrot")
                                .withSubtype(HotPeppers.class, "hotpeppers")
                )
                .build();

        // Now Moshi is set up to disambiguate ingredients. If we add new ingredients, we need to add them above,
        // or else come up with a cleaner way to register new ingredients without changing the code...
        //  (It's almost like that's a very common use-case in OOP. ;-))

        // One more step is needed. We can't just use an adapter like this:
        // JsonAdapter<Ingredient> ingredientAdapter = moshi.adapter(Ingredient.class).nonNull();
        // ...because we're expecting a JSON *array* of ingredients. So we need a layer that handles the enclosing list.

        // Since List is generic, we shouldn't just pass List.class to the adapter factory.
        // Instead, let's be more precise. Java has built-in classes for talking about generic types programmatically.
        // Building libraries that use them is outside the scope of this class, but we'll follow the Moshi docs'
        // template by creating a Type object corresponding to List<Ingredient>:
        Type listOfIngredientsType = Types.newParameterizedType(List.class, Ingredient.class);
        // ...and pass it instead of List.class:
        JsonAdapter<List<Ingredient>> recipeAdapter = moshi.adapter(listOfIngredientsType);
        // ...and finally read the json string:
        try {
            List<Ingredient> recipe = recipeAdapter.fromJson(jsonList);
            // In the beginning, the soup is empty. There's nothing in the pot.
            Soup result = new Soup(timidChef);

            // I suppose we'd better actually add the ingredients to the soup, too.
            for(Ingredient i : recipe) {
                // We could stir the ingredients into the soup like this:
                // result.stirIn(i);
                // but doing that would skip the checking we've implemented in the ingredient classes. Instead:
                i.add(result);
            }
            return result;
        }
        // From the Moshi Docs:
        //   "Moshi always throws a standard java.io.IOException if there is an error reading the JSON document, or if
        //    it is malformed. It throws a JsonDataException if the JSON document is well-formed, but doesn't match the
        //    expected format."    (https://github.com/square/moshi)
        catch(IOException e) {
            // In a real system, we wouldn't println like this, but it's useful for demonstration:
            System.err.println("SoupHandler: string wasn't valid JSON.");
            throw e;
        } catch(JsonDataException e) {
            // Note that JsonDataException extends *RuntimeException* -- it's unchecked! I like to add it to the
            // "throws" clause anyway, for the sake of documentation.
            // In a real system, we wouldn't println like this, but it's useful for demonstration:
            System.err.println("SoupHandler: JSON wasn't an ingredient.");
            throw e;
        }
    }

    /**
     * Serializes the ingredients in a bowl soup into a Json object for sending
     * across the 'net. The result does not include the chef's timidity or
     * any other soup properties---we're just sending the ingredients.
     *
     * @param soup The soup to serialize into a String
     * @return the serialized Json list representing the ingredients
     */
    public static String toJson(Soup soup) {
        // as in fromJson, we need to work with arbitrary Ingredients.
        // The polymorphic factory will automatically _insert_ the "type" field
        Moshi moshi = new Moshi.Builder()
                .add(
                        // Expect something that's an Ingredient...
                        PolymorphicJsonAdapterFactory.of(Ingredient.class, "type")
                                // ...with two possibilities for concrete shapes, disambiguated by type:
                                .withSubtype(Carrots.class, "carrot")
                                .withSubtype(HotPeppers.class, "hotpeppers")
                )
                .build();

        // Although the Java object contains a set, there's no analogue in Json.
        // Moshi will serialize this set as a list.
        Type setOfIngredientsType = Types.newParameterizedType(Set.class, Ingredient.class);
        JsonAdapter<Set<Ingredient>> adapter = moshi.adapter(setOfIngredientsType);
        return adapter.toJson(soup.ingredients());
    }
}
