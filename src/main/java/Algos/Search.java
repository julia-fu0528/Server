package Algos;

import java.util.List;

public class Search {

  private List<List<String>> parsed;


  /**
   * Constructor for Search taking in only the parsed CSV
   *
   * @param parsed parsed CSV as a 2d list of Strings
   */
  public Search(List<List<String>> parsed) {
    this.parsed = parsed;
  }

  /**
   * Searches for a target and takes in only the string to look for (no column specified)
   *
   * @param target String to look for
   */

  public boolean searchTarget(String target) {
    //check if csv or target is empty and if so return false and print error
    if (this.parsed.size() == 0) {
      System.err.println("Empty CSV Parse");
      return false;
    } else if (target.length() == 0) {
      System.err.println("Empty search target");
      return false;
    } else {
      //go through 2d list and search for target
      for (int i = 0; i < this.parsed.size(); i++) {
        for (int j = 0; j < this.parsed.get(i).size(); j++) {
          if (this.parsed.get(i).get(j).equals(target)) {
            //if found print and return true
            System.out.println("Target was found in row " + (i + 1));
            return true;
          }
        }
      }
      //if not found print and return false
      System.out.println("Target could not be found.");
      return false;
    }
  }

  /**
   * Searches for a target and takes in  the string to look for and column specified
   *
   * @param target String to look for
   * @param column String of column to look in (either a number or header title)
   */
  public boolean searchTarget(String target, String column) {
    //check if csv or target is empty and if so return false and print error
    if (this.parsed.size() == 0) {
      System.err.println("Empty CSV Parse");
      return false;
    } else if (target.length() == 0) {
      System.err.println("Empty search target");
      return false;
    } else {
      //check if the column input is given as an integer or header
      if (isNumeric(column)) {
        //if integer make sure it is inbounds
        int c = Integer.parseInt(column);
        if (c < 0 || c > this.parsed.get(0).size()) {
          System.err.println("Column out of bounds");
          return false;
        }
        //search through rows in the given column for target
        for (int i = 0; i < this.parsed.size(); i++) {
          if (this.parsed.get(i).get(c).equals(target)) {
            //if found print and return true
            System.out.println("Target was found in row " + (i + 1));
            return true;
          }
        }
      } else {
        //else check for the header and get the corresponding column number
        List<String> headers = this.parsed.get(0);
        int c = checkHeaders(headers, column);
        //if header does not exist print error and return false
        if (c == -1) {
          System.err.println("Header not found ");
          return false;
        }
        //search through rows in the given column for target (starting after headers)
        for (int i = 1; i < this.parsed.size(); i++) {
          if (this.parsed.get(i).get(c).equals(target)) {
            System.out.println("Target was found in row " + (i + 1));
            return true;
          }
        }
      }
      //if not found print error return false
      System.out.println("Target could not be found.");
      return false;
    }
  }

  /**
   * Checks to see if the given column header is in fact a header and returns its index (-1 if not found)
   *
   * @param headers List of Strings representing the headers
   * @param column String of column title to look for
   */
  private int checkHeaders(List<String> headers, String column) {
    for (int i = 0; i < headers.size(); i++) {
      if (headers.get(i).equals(column)) {
        return i;
      }
    }
    return -1;
  }


  /**
   * Checks if a String is an Integer (returns true if so, false if not)
   *
   * @param str String to check if numeric
   *
   */
  private boolean isNumeric(String str) {
    try {
      Integer.parseInt(str);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
