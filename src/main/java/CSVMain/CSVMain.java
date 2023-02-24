package CSVMain;

import Algos.CSVParser;
import Algos.Search;
import RowCreator.ListCreator;

import java.io.FileReader;
import java.io.Reader;
import java.util.List;

/** The Main class of our project. This is where execution begins. */
public final class CSVMain {
  private String[] args;

  /**
   * The initial method called when execution begins.
   *
   * @param args An array of command line arguments
   */
  public static void main(String[] args) {
    new CSVMain(args).run();
  }

  private CSVMain(String[] args) {
    this.args = args;
  }

  /**
   * Deals with command line input to facilitate the program by running the csv parser and search methods
   */
  private void run() {
    //less than 2 and greater than 3 arguments is an invalid input->print error
    if (this.args.length < 2 || this.args.length > 3) {
      System.err.println(
          "Invalid number of arguments. Input must follow the following structure:\n"
              + "filepath, search term or filepath, search term, column identifier");
    } else {
      try {
        //pass in the filepath to the reader and then the reade into the CSV parser
        Reader reader = new FileReader(args[0]);
        CSVParser parser = new CSVParser(reader, new ListCreator());
        //parse the CSV file
        List<List<String>> parsedList = parser.parse();
        Search searcher = new Search(parsedList);
        //if only 2 arguments, input column is not specified->use generic searchTarget (2 args) to search
        if (this.args.length == 2) {
          searcher.searchTarget(args[1]);
        }
        //if 3 arguments input, column is specified->use special searchTarget (3 args) to search and pass in column
        else {
          String column = args[2];
          searcher.searchTarget(args[1], column);
        }
      } catch (Exception e) {
        System.err.println(e);
      }
    }
  }
}
