package Handlers;

import Algos.CSVParser;
import Exceptions.FileNotFoundException;
import RowCreator.ListCreator;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import edu.brown.cs32.examples.moshiExample.ingredients.Carrots;
import edu.brown.cs32.examples.moshiExample.ingredients.HotPeppers;
import edu.brown.cs32.examples.moshiExample.ingredients.Ingredient;
import edu.brown.cs32.examples.moshiExample.server.LoadedFiles;
import edu.brown.cs32.examples.moshiExample.soup.Soup;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.List;
import java.util.Set;

/**
 * Handler class for the soup ordering API endpoint.
 *
 * This endpoint is similar to the endpoint(s) you'll need to create for Sprint 2. It takes a basic GET request with
 * no Json body, and returns a Json object in reply. The responses are more complex, but this should serve as a reference.
 *
 */
public class LoadHandler implements Route {
    public LoadedFiles<List<List<String>>> loaded;

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
        String filepath = request.queryParams("path");
        if (filepath == null){
            return new MissingFilePathResponse().serialize();
        }
        //String filepath = qm.value("path");
        System.out.println(filepath);
        FileReader toParse = null;
        try{
            toParse = new FileReader(filepath);
        }catch(FileSystemNotFoundException e){
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


    /**
     * Response object to send if someone requested soup before any recipes were loaded
     */
    public record InaccessibleCSVResponse(String response_type, String filepath, String message){
        public InaccessibleCSVResponse(String filepath){
            this("error_datasource", filepath, "File '" + filepath + "'doesn't exist. ");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(InaccessibleCSVResponse.class).toJson(this);
        }
    }
    public record MissingFilePathResponse(String response_type, String message) {
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

    public record CSVParsingFailureResponse(String response_type, String filepath, String message){
        public CSVParsingFailureResponse(String filepath){
            this("error_datasource", filepath, "Error parsing" + filepath);
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(CSVParsingFailureResponse.class).toJson(this);
        }
    }
    public record CSVParsingSuccessResponse(String response_type, String filepath, String message){
        public CSVParsingSuccessResponse(String filepath){
            this("success", filepath, "CSV File'" + filepath + "' successfully stored. " +
                    "Contents accessible in endpoint viewcsv");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(CSVParsingSuccessResponse.class).toJson(this);
        }
    }
}