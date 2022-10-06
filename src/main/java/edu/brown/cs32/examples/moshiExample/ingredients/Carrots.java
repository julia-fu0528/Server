package edu.brown.cs32.examples.moshiExample.ingredients;

import edu.brown.cs32.examples.moshiExample.soup.ActualFlavorException;
import edu.brown.cs32.examples.moshiExample.soup.Soup;

/**
 * A carrot ingredient, meant to be created when deserializing a JSON recipe.
 *
 * Using records where possible, because they provide a nice auto-definition of equals, toString, etc.
 * They are also immutable. Since they are classes, they can implement interfaces. But, since they are
 * classes that extend Record, they cannot extend other classes (since Java has no multiple inheritance).
 *
 * chop: what shape are the carrots being added in?
 * amount: how much of this pepper is being added? (See comment below)
 *
 */
public record Carrots(CarrotChopType chop, double amount) implements Ingredient {

    public enum CarrotChopType {SHREDDED, MATCHSTICK, UNCHOPPED}

    // Note: the idea of "amounts" is oversimplified here. In reality, we'd want to know what the units were.
    // But even more, in real cooking, we often have to deal with measurement by weight vs. measurement by volume.

    @Override
    public void add(Soup soup) throws ActualFlavorException {
        // Carrots are always OK.
        soup.stirIn(this);
    }

}
