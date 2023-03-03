package Servers;

import static spark.Spark.after;

import Handlers.LoadHandler;
import Handlers.SearchHandler;
import Handlers.ViewHandler;
import Weather.Requester.PlainRequester;
import Weather.WeatherCachingProxy;
import Weather.WeatherHandler;
import spark.Spark;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Top-level class for this sprint. Contains the main() method which starts Spark and runs the various handlers.
 *
 * We have four endpoints in this demo. They need to share state (a menu).
 * This is a great chance to use dependency injection, as we do here with the menu set. If we needed more endpoints,
 * more functionality classes, etc. we could make sure they all had the same shared state.
 */
public class Server {
    public static void main(String[] args) {
        Set<List<List<String>>> storage = new HashSet<>();
        Spark.port(3232);
        after((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "*");
        });

        // Setting up the handler for the four different endpoints
        Spark.get("loadcsv", new LoadHandler(storage));
        Spark.get("viewcsv", new ViewHandler(storage));
        Spark.get("searchcsv", new SearchHandler(storage));
        Spark.get("weather", new WeatherHandler());
        Spark.init();
        Spark.awaitInitialization();
        System.out.println("Server started.");
    }
}
