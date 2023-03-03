package CSV.Algos;

import CSV.RowCreators.RowCreator.CreatorFromRow;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVParser<T> {
  private CreatorFromRow<T> type;
  private BufferedReader br;

  /**
   * Constructor for the CSVParser
   *
   * @param reader A reader file representing the CSV
   * @param type The type of CreatorFromRow child class which specifies the type of each row of data
   */
  public CSVParser(Reader reader, CreatorFromRow<T> type) {
    this.br = new BufferedReader(reader);
    this.type = type;
  }

  /**
   * Parses the CSV into desired data type
   *
   */
  public List<T> parse() {
    List<T> list = new ArrayList<>();
    try {
      String line = "";
      //loop through each line of the CSV until the end
      while ((line = this.br.readLine()) != null) {
        //split at the commas to create individual items in an array
        String[] columns = line.split(",");
        List<String> lineList = Arrays.stream(columns).toList();
        //make a list of each rows' items ignoring items that are only whitespace
        List<String> copy = new ArrayList<>();
        for (int i = 0; i < lineList.size(); i++) {
          if (!lineList.get(i).isEmpty()) {
            copy.add(lineList.get(i));
          }
        }

        //add the row as whatever datatype to the overall list representing the parsed CSV
        list.add(this.type.create(copy));
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    return list;
  }
}
