package MockedData;

import CSV.Algos.CSVParser;
import CSV.RowCreators.RowCreator.ListCreator;
import Servers.LoadedFiles;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

public class MockedCSV {
    public static String invalid_query = "loadcsv?filepath=starsdatas.csv";
    public static String invalid_filepath = "starsdatas.csv";
    public static String emptycsv_query = "loadcsv?filepath=src/main/data/made-example-files/empty.csv";
    public static String emptycsv_path = "src/main/data/made-example-files/empty.csv";

    public static String stardata_query = "loadcsv?filepath=src/main/data/stars/stardata.csv";
    public static String stardata_path = "src/main/data/stars/stardata.csv";

//    public static LoadedFiles empty_file;
//
//    static {
//        try {
//            empty_file = new LoadedFiles<List<List<String>>>();
//            empty_file.storeFile(new CSVParser(new FileReader("empty.csv"),new ListCreator()).parse());
//            List<List<String>> file = empty_file.storage;
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
