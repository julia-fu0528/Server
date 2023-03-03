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
 * Handler class for the soup ordering API endpoint.
 *
 * This endpoint is similar to the endpoint(s) you'll need to create for Sprint 2. It takes a basic GET request with
 * no Json body, and returns a Json object in reply. The responses are more complex, but this should serve as a reference.
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
     * Pick a convenient soup and make it. the most "convenient" soup is the first recipe we find in the unordered
     * set of recipe cards.
     * @param request the request to handle
     * @param response use to modify properties of the response
     * @return response content
     * @throws Exception This is part of the interface; we don't have to throw anything.
     */
    @Override
    public Object handle(Request request, Response response) throws Exception {
        // Request: when the client asks for something from the webpage (what your user sends)
        // Response: when the webpage gives sth. back (what you're constructing to return
        // QueryParamsMap qm = request.queryMap();
        String filepath = request.queryParams("filepath");
        if (filepath == null){
            return new MissingFilePathResponse().serialize();
        }
        //String filepath = qm.value("path");
        System.out.println(filepath);
        FileReader toParse;
        try{
            toParse = new FileReader(filepath);
        }catch(Exception e){
            return new InaccessibleCSVResponse(filepath).serialize();
        }

        //BufferedReader fr = new BufferedReader(new FileReader(filepath));
        CSVParser parser = new CSVParser(toParse, new ListCreator());
        List<List<String>> csv_json;
        try {
            csv_json = parser.parse();
        } catch (Exception e) {
            return new CSVParsingFailureResponse(filepath).serialize();
        }
        this.loaded.storeFile(csv_json);
        return new CSVParsingSuccessResponse(filepath).serialize();
        //return new SoupSuccessResponse(soup.ingredients()).serialize();
        //return new SoupNoRecipesFailureResponse().serialize();
        // NOTE: beware this "return Object" and "throws Exception" idiom. We need to follow it because
        //   the library uses it, but in general this is lowers the protection of the type system.
    }
    /**
     * Response object to send, containing a soup with certain ingredients in it
     */
    public void setLoaded(LoadedFiles<List<List<String>>> csv){
        this.loaded = csv;
    }

    /**
     * Response object to send if someone requested soup before any recipes were loaded
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

    public record CSVParsingFailureResponse(String result, String filepath, String message){
        public CSVParsingFailureResponse(String filepath){
            this("error_datasource", filepath, "Error parsing" + filepath);
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(CSVParsingFailureResponse.class).toJson(this);
        }
    }
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
            // For debugging purposes, show in the console _why_ this fails
            // Otherwise we'll just get an error 500 from the API in integration
            // testing.
            e.printStackTrace();
            throw e;
            }
        }
    }
}
