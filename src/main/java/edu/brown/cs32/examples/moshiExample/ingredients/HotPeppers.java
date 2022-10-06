package edu.brown.cs32.examples.moshiExample.ingredients;

import edu.brown.cs32.examples.moshiExample.soup.ActualFlavorException;
import edu.brown.cs32.examples.moshiExample.soup.Soup;

/**
 * A hot pepper ingredient, meant to be created when deserializing a JSON recipe.
 *
 * Using records where possible, because they provide a nice auto-definition of equals, toString, etc.
 * They are also immutable. Since they are classes, they can implement interfaces. But, since they are
 * classes that extend Record, they cannot extend other classes (since Java has no multiple inheritance).
 *
 * We'll create instances of this class via Moshi. Importantly, because Moshi deserializes into fields,
 * we won't add any extra state (i.e., we won't save the view of the soup's ingredients).
 *
 * scovilles: how hot is the pepper? in Scoville units
 * amount: how much of this pepper is being added? (See comment below)
 *
 */
public record HotPeppers(int scovilles, double amount) implements Ingredient {

    static final long spicyLimit = 1000;

    // Note: the idea of "amounts" is oversimplified here. In reality, we'd want to know what the units were.
    // But even more, in real cooking, we often have to deal with measurement by weight vs. measurement by volume.

    @Override
    public void add(Soup soup) throws ActualFlavorException {
        // Would this be too spicy?
        // (Summing Scovilles is not a good way to compute spiciness in a real soup, but serves as an example.)
        if(soup.isTimidChef() && scovilles > spicyLimit)
            throw new ActualFlavorException("that would be too spicy!");
        soup.stirIn(this);
    }

}
