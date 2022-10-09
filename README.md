# Serializing and Deserializing Json 

This is a rough example of:
* using the Moshi Json library to serialize/deserialize; and 
* using the SparkJava library to run an API server. 

It should contain most of the programmatic reference you need to do Sprint 2. E.g.:
* running an API server;
* integration/system testing an API server via HTTP requests;
* serializing/deserializing classes;
* serializing/deserializing generic classes; and
* serializing/deserializing with polymorphism. 

However, keep in mind that this code isn't _exactly_ what you need for Sprint 2. We expect you to use this as a reference, but not to borrow too heavily (you'll need to work with different types of Json response, requests, etc.).

Historical note: 0320 had previously used Gson for serializing/deserializing Json, but Gson seems to now be in maintainance mode, and doesn't support records well. So we switched for Fall 2022.

## Setup 

You'll need two dependencies for handling Json: `moshi` and `moshi-adapters`, both from `com.squareup.moshi`. You should be able to copy and paste them from the `pom.xml` of this project. Similarly, you'll depend on SparkJava: `spark-core` from `com.sparkjava`. 

## Demo Framing

We're building an application that cooks recipes automatically. Since this is an early prototype, all recipes are assumed to be soups. Likewise, there are only two things you can make soup with in the application today: carrots, and hot peppers. 

The application is an API server that responds to "order" requests. The response is a serialized set of ingredients. In its current form, the response is always an arbitrary recipe (if any exists). If no
recipe exists, the server replies with an error response. 

#### Ingredients

Ingredients are Json objects. They _always_ have a "type" field, which indicates the type of ingredient (e.g. "carrots"). Other fields can vary depending on what kind of ingredient it is.

```json
{
  "type": ...,
  ...
}
```

In this demo, the `type` field is either `carrots` or `hotpeppers`. 

Carrots and hot peppers both have `amount` fields. Hot peppers also have a `scovilles` field, which measures their spiciness. Each type of ingredient defines its own custom method for adding it to the soup. In our demo, the rule for hot peppers is that, if the chef is timid, they can't be too hot. 

## Running 

You can run the example by executing the `Server` class `main` method. This starts up a real webserver on your computer. By default, it's set to use port `3232`, so you should be able (while the server is running!) to send requests via `localhost:3232` in your browser. The endpoint is `order`, so `localhost:3232/order` will produce an order result (or an error).

There are also two test classes.

## Exercise 

### Run and Query

Run the `Server` and confirm that you are able to make web queries from your browser. 

### Modification

Right now, the `order` request produces whichever set of ingredients appears first in the menu set. How would you modify the code so that an `order` could contain the name of a soup, and that soup could be retrieved? 

 Hint: You can get a _parameter_ of the request via `request.queryParams()` in the handler method. E.g., if you call the parameter `soupname`, you'd use `request.queryParams("soupname")`. Note that this was originally written with a typo, referring to the `params` method rather than `queryParams`. These are different. Ed post 430 corrected the issue. 
 
Remember also that you're allowed to modify this code, but you shouldn't need to alter most classes (or even really understand them yet). What _do_ you need to modify? 

### Testing

What other integration tests should have been written in the `edu.brown.cs32.examples.moshiExample.TestSoupAPIHandlers` class?

Add at least one. 

## Additional Info for Sprint 2

You can deserialize a Json object into a Java object with fewer fields. E.g., if you've got a Json object with 26 different fields:

```json
{
  "A": 1,
  "B": 2,
  ...
}
```

You can deserialize this into an object with only `"A"` and `"B"` fields. This is useful when processing very large, verbose response Json from other APIs, but only need a few fields. 
