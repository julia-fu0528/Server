package Handlers;

import Exceptions.NoFileStoredException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;
import edu.brown.cs32.examples.moshiExample.server.LoadedFiles;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;

public class ViewHandler implements Route{
    public LoadedFiles<List<List<String>>> loaded;

    public ViewHandler(){
        this.loaded = LoadHandler.loaded;

    }
    public Object handle(Request request, Response response) throws Exception{
        List<List<String>> csv_json;
        try{
            csv_json = this.loaded.getFile();
            return new ViewCSVSuccessResponse(csv_json).serialize();
        }catch(NoFileStoredException e){
            return new ViewCSVFailureResponse().serialize();
        }
    }
    public void setLoaded(LoadedFiles<List<List<String>>> csv){
        this.loaded = csv;
    }
    public record ViewCSVSuccessResponse(String response_type, List<List<String>> data, String message){
        public ViewCSVSuccessResponse(List<List<String>> data){
            this("success", data, "File available for view.");
        }
        String serialize(){
            try{
                Moshi moshi = new Moshi.Builder().build();
                return moshi.adapter(ViewCSVSuccessResponse.class).toJson(this);
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
    public record ViewCSVFailureResponse(String response_type, String message){
        public ViewCSVFailureResponse(){
            this("error_datasource", "No CSV file stored yet.");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(ViewCSVFailureResponse.class).toJson(this);
        }
    }
}
