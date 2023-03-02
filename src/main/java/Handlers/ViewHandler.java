package Handlers;

import Exceptions.NoFileStoredException;
import com.squareup.moshi.Moshi;
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
    public record ViewCSVSuccessResponse(String reponse_type, List<List<String>> parsed, String message){
        public ViewCSVSuccessResponse(List<List<String>> parsed){
            this("success", parsed, "File available for view.");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(ViewCSVSuccessResponse.class).toJson(this);
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
