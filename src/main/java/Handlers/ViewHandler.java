package Handlers;

import Exceptions.NoFileStoredException;
import com.squareup.moshi.Moshi;
import Servers.LoadedFiles;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;
/**
 * Handler class for the viewcsv API endpoint.
 *
 */
public class ViewHandler implements Route{
    public LoadedFiles<List<List<String>>> loaded;
    /**
     * Constructor accepts some shared state
     */
    public ViewHandler(){
        this.loaded = LoadHandler.loaded;

    }
    public ViewHandler(LoadedFiles<List<List<String>>> loaded){
        this.loaded = loaded;
    }
    /**
     * handles the csv file to view
     * @param request the request to handle
     * @param response use to modify properties of the response
     * @return response content
     * @throws Exception This is part of the interface; we don't have to throw anything.
     */
    public Object handle(Request request, Response response) throws Exception{
        // retrieves the csv file
        List<List<String>> csv_json;
        try{
            csv_json = this.loaded.getFile();
            return new ViewCSVSuccessResponse(csv_json).serialize();
        }catch(NoFileStoredException e){
            // if no csv has been loaded
            return new ViewCSVFailureResponse().serialize();
        }
    }
    /**
     * Response object to send if viewing csv succees
     */
    public record ViewCSVSuccessResponse(String result, List<List<String>> data, String message){
        public ViewCSVSuccessResponse(List<List<String>> data){

            this("success", data, "File available for view.");
        }
        String serialize(){
            try{
                Moshi moshi = new Moshi.Builder().build();
                return moshi.adapter(ViewCSVSuccessResponse.class).toJson(this);
            }
            catch(Exception e) {
                // error
                e.printStackTrace();
                throw e;
            }
        }

    }
    /**
     * Response object to send if no csv file has been stored yet.
     */
    public record ViewCSVFailureResponse(String result, String message){
        public ViewCSVFailureResponse(){
            this("error_datasource", "No CSV file stored yet.");
        }
        String serialize(){
            Moshi moshi = new Moshi.Builder().build();
            return moshi.adapter(ViewCSVFailureResponse.class).toJson(this);
        }
    }
}
