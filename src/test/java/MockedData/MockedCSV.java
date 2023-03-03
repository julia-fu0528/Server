package MockedData;

import CSV.Algos.CSVParser;
import CSV.Algos.Search;
import CSV.RowCreators.RowCreator.ListCreator;
import Exceptions.SearchFailureException;
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

    public static String headers_empty_query = "loadcsv?filepath=src/main/data/made-example-files/empty-with-headers.csv";
    public static String headers_empty_path = "src/main/data/made-example-files/empty-with-headers.csv";
    public static String noheaders_people_query = "loadcsv?filepath=src/main/data/made-example-files/people-no-headers.csv";
    public static String noheaders_people_path = "src/main/data/made-example-files/people-no-headers.csv";

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
    public static List<List<String>> empty_file;

    static {
        try {
            empty_file = new CSVParser(new FileReader(emptycsv_path), new ListCreator()).parse();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static List<List<String>> star_file;

    static {
        try {
            star_file = new CSVParser(new FileReader(stardata_path), new ListCreator()).parse();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static List<List<String>> star_searched;

    static {
        try {
            star_searched = new Search(star_file).searchTarget("ProperName", "Rory");
        } catch (SearchFailureException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<List<String>> headers_empty_file;

    static {
        try {
            headers_empty_file = new CSVParser(new FileReader(headers_empty_path), new ListCreator()).parse();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static List<List<String>> noheaders_people_file;

    static {
        try {
            noheaders_people_file = new CSVParser(new FileReader(noheaders_people_path), new ListCreator()).parse();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static List<List<String>> people_searched;

    static {
        try {
            people_searched = new Search(noheaders_people_file).searchTarget("3", "student");
        } catch (SearchFailureException e) {
            throw new RuntimeException(e);
        }
    }

    public MockedCSV() throws FileNotFoundException {
    }
}
