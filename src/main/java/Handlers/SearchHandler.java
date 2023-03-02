package Handlers;

import Algos.Search;
import Exceptions.SearchFailureException;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import edu.brown.cs32.examples.moshiExample.server.LoadedFiles;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;

public class SearchHandler implements Route {
    public LoadedFiles<List<List<String>>> loaded;

    public SearchHandler() {
        this.loaded = LoadHandler.loaded;
    }

    public Object handle(Request request, Response response) throws Exception {
        String column = request.queryParams("column");
        String value = request.queryParams("value");
        List<List<String>> data;
        if (column == null) {
            return new MissingColumnResponse().serialize();
        } else if (value == null) {
            return new MissingValueResponse().serialize();
        }
        try {
            Search searcher = new Search(this.loaded.storage);
            data = searcher.searchTarget(column, value);
        } catch (SearchFailureException e) {
            return new SearchFailureResponse(column, value).serialize();
        }
        if (data.isEmpty()){
            return new ValueNotFoundResponse(column, value).serialize();
        }
        return new SearchSuccessResponse(data, column, value).serialize();
    }
//    public void setLoaded(LoadedFiles<List<List<String>>> csv){
//        this.loaded = csv;
//    }
    public record MissingColumnResponse(String response_type, String message) {
        public MissingColumnResponse() {
            this("error_bad_request", "Missing column query");
        }

        String serialize() {
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(MissingColumnResponse.class).toJson(this);
        }
    }
    public record MissingValueResponse(String response_type, String message){
        public MissingValueResponse(){
           this("error_bad_request", "Missing value query");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(MissingValueResponse.class).toJson(this);
        }
    }
    public record SearchFailureResponse(String response_type, String column, String value, String message) {
        public SearchFailureResponse(String column, String value) {
            this("error_datasource", column, value,
                    "the value'" + value + "can't be found at column'" + column);
        }

        String serialize() {
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(SearchFailureResponse.class).toJson(this);
        }
    }
    public record SearchSuccessResponse(String response_type, List<List<String>> data, String column, String value, String message){
        public SearchSuccessResponse(List<List<String>> data, String column, String value){
            this("success", data, column, value, "Value successfully searched");
        }
        String serialize(){
            try {
                Moshi moshi = new Moshi.Builder().build();
                return moshi.adapter(SearchSuccessResponse.class).toJson(this);
            }
            catch(Exception e) {
                // For debugging purposes, show in the console _why_ this fails
                // Otherwise we'll just get an error 500 from the API in integration
                // testing.
                e.printStackTrace();
                throw e;
            }
        }
    }
    public record ValueNotFoundResponse(String response_type, String column, String value, String message){
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

