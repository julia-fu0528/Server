package edu.brown.cs32.examples.moshiExample.soup;

/**
 * Prevent adding an ingredient because it would be a terrible idea.
 * The name comes from the fact that FlavorException is already used by another library.
 */
public class ActualFlavorException extends Exception {
    public ActualFlavorException(String message) {
        super(message);
    }
}
