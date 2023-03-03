package Handlers;

import CSV.Algos.CSVParser;
import CSV.RowCreators.RowCreator.ListCreator;
import com.squareup.moshi.Moshi;
import Servers.LoadedFiles;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.FileReader;
import java.nio.file.FileSystemNotFoundException;
import java.util.List;

/**
 * Handler class for the loadcsv API endpoint.
 *
 */
public class LoadHandler implements Route {
    public static LoadedFiles<List<List<String>>> loaded;

    /**
     * Constructor accepts some shared state
     */
    public LoadHandler(LoadedFiles<List<List<String>>> loaded) {
        this.loaded = loaded;
    }

    /**
     * handles the csv file to load
     * @param request the request to handle
     * @param response use to modify properties of the response
     * @return response content
     * @throws Exception This is part of the interface; we don't have to throw anything.
     */
    @Override
    public Object handle(Request request, Response response) throws Exception {
        // gets the value under the query key "filepath"
        String filepath = request.queryParams("filepath");
        if (filepath == null){
            // no filepath query
            return new MissingFilePathResponse().serialize();
        }
        System.out.println(filepath);
        FileReader toParse;
        try{
            toParse = new FileReader(filepath);
        }catch(Exception e){
            // parse failure
            return new InaccessibleCSVResponse(filepath).serialize();
        }

        CSVParser parser = new CSVParser(toParse, new ListCreator());
        List<List<String>> csv_json;
        try {
            csv_json = parser.parse();
        } catch (Exception e) {
            // parse failure
            return new CSVParsingFailureResponse(filepath).serialize();
        }
        this.loaded.storeFile(csv_json);
        // parse success
        return new CSVParsingSuccessResponse(filepath).serialize();
    }
    /**
     * Set the instance variable loaded with a LoadedFile
     */
    public void setLoaded(LoadedFiles<List<List<String>>> csv){
        this.loaded = csv;
    }

    /**
     * Response object to send if the filepath is invalid
     */
    public record InaccessibleCSVResponse(String result, String filepath, String message){
        public InaccessibleCSVResponse(String filepath){
            this("error_datasource", filepath, "File '" + filepath + "'doesn't exist. ");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(InaccessibleCSVResponse.class).toJson(this);
        }
    }

    /**
     * Response object to send if no filepath query is entered
     * @param result String signalling error
     * @param message String specifying error
     */
    public record MissingFilePathResponse(String result, String message) {
        public MissingFilePathResponse() {
            this("error_bad_request", "Missing filepath query.");
        }

        /**
         * @return this response, serialized as Json
         */
        String serialize() {
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(MissingFilePathResponse.class).toJson(this);
        }
    }

    /**
     * Response object to send if the csv file parsing fails
     * @param result String signalling error
     * @param filepath query value entered by the user
     * @param message String specifying error
     */
    public record CSVParsingFailureResponse(String result, String filepath, String message){
        public CSVParsingFailureResponse(String filepath){
            this("error_datasource", filepath, "Error parsing" + filepath);
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(CSVParsingFailureResponse.class).toJson(this);
        }
    }

    /**
     * Response object to send if the csv file is successfully parsed and laoded
     * @param result String signalling success
     * @param filepath query value entered by the user
     * @param message String reporting success
     */
    public record CSVParsingSuccessResponse(String result, String filepath, String message){
        public CSVParsingSuccessResponse(String filepath){
            this("success", filepath, "CSV File'" + filepath + "' successfully stored. " +
                    "Contents accessible in endpoint viewcsv");
        }
        String serialize(){
            try {
                Moshi moshi = new Moshi.Builder().build();
                return moshi.adapter(CSVParsingSuccessResponse.class).toJson(this);
            } catch(Exception e) {
                // internal error
            e.printStackTrace();
            throw e;
            }
        }
    }
}
