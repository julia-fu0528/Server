package Handlers;

import Algos.Search;
import edu.brown.cs32.examples.moshiExample.server.LoadedFiles;
import spark.Route;

import java.util.List;

public class SearchHandler implements Route {
    public LoadedFiles<List<List<String>>> loaded;
    public SearchHandler(){

    }
    public Object handle() throws Exception{
        List<List<String>> searched;
        try{
            Search searcher = new Search(this.loaded.storage);
            searched = searcher.searchTarget()
        }
    }
}
