package Handlers;

import CSV.Algos.Search;
import Exceptions.SearchFailureException;
import com.squareup.moshi.Moshi;
import Servers.LoadedFiles;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;
import java.util.Set;

/**
 * Handler class for the searchcsv API endpoint.
 *
 */
public class SearchHandler implements Route {
    public Set<List<List<String>>> loaded;
    /**
     * Constructor accepts some shared state
     */
    public SearchHandler(Set<List<List<String>>> loaded) {
        this.loaded = loaded;
    }
    /**
     * handles the csv file to view
     * @param request the request to handle
     * @param response use to modify properties of the response
     * @return response content
     * @throws Exception This is part of the interface; we don't have to throw anything.
     */
    public Object handle(Request request, Response response) throws Exception {
        // retrieves queries entered by the user
        String column = request.queryParams("column");
        String value = request.queryParams("value");
        List<List<String>> data;
        // if no column query is provided by the user
        if (column == null) {
            return new MissingColumnResponse().serialize();
            // if no value query is not provided by the user
        } else if (value == null) {
            return new MissingValueResponse().serialize();
        }
        try {
            Search searcher = new Search(this.loaded.iterator().next());
            data = searcher.searchTarget(column, value);
        } catch (SearchFailureException e) {
            // if searching fails
            return new SearchFailureResponse(column, value).serialize();
        }
        if (data.isEmpty()){
            // if value provided by the user is not found in the given column
            return new ValueNotFoundResponse(column, value).serialize();
        }
        // search success
        return new SearchSuccessResponse(data, column, value).serialize();
    }
    /**
     * Response object to send if the column query is not given by the user
     */
    public record MissingColumnResponse(String result, String message) {
        public MissingColumnResponse() {
            this("error_bad_request", "Missing column query");
        }

        String serialize() {
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(MissingColumnResponse.class).toJson(this);
        }
    }
    /**
     * Response object to send if the value query is not given by the user
     */
    public record MissingValueResponse(String result, String message){
        public MissingValueResponse(){
           this("error_bad_request", "Missing value query");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(MissingValueResponse.class).toJson(this);
        }
    }
    /**
     * Response object to send if search fails
     */
    public record SearchFailureResponse(String result, String column, String value, String message) {
        public SearchFailureResponse(String column, String value) {
            this("error_datasource", column, value,
                    "Searching '" + value + " ' at column '" + column + "' fails");
        }

        String serialize() {
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(SearchFailureResponse.class).toJson(this);
        }
    }
    /**
     * Response object to send if search succeeds
     */
    public record SearchSuccessResponse(String result, List<List<String>> data, String column, String value, String message){
        public SearchSuccessResponse(List<List<String>> data, String column, String value){
            this("success", data, column, value, "Value successfully searched");
        }
        String serialize(){
            try {
                Moshi moshi = new Moshi.Builder().build();
                return moshi.adapter(SearchSuccessResponse.class).toJson(this);
            }
            catch(Exception e) {
                // other errors
                e.printStackTrace();
                throw e;
            }
        }
    }
    /**
     * Response object to send if the value to be searched is not found
     */
    public record ValueNotFoundResponse(String result, String column, String value, String message){
        public ValueNotFoundResponse(String column, String value){
            this("error.json", column, value,
                    "The value ' " + value + "' can't be found at column " + column);
        }
        String serialize(){
            try{
                Moshi moshi = new Moshi.Builder().build();
                return moshi.adapter(ValueNotFoundResponse.class).toJson(this);
            }catch(Exception e){
                e.printStackTrace();
                throw e;
            }
        }
    }
}

