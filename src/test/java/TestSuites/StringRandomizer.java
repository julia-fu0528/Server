package TestSuites;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Helper class for randomizing a string using fuzz testing
 */
public class StringRandomizer {
    /**
     * Constructor for the StringRandomizer class
     */
    public StringRandomizer() {
    }

    /**
     * Function generates a random string of length 0 to 27 with characters a-z (all cases), and spaces
     * @return a randomized string
     */
    public String generateRandomString() {
        int length = new Random().nextInt(27); // generate a random length between 0 and 26
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ "; // define the characters to use
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = new Random().nextInt(characters.length()); // generate a random index within the range of the character string
            sb.append(characters.charAt(index)); // add the character at the generated index to the string
        }
        return sb.toString();
    }
    public String generateRandomValidCol() {
        List<String> validCols;
        validCols = List.of("StarID", "ProperName","X","Y","Z");
        int index = new Random().nextInt(5);
        return validCols.get(index);
    }
}
