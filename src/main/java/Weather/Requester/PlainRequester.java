package Weather.Requester;

import com.squareup.moshi.Moshi;
import okio.Buffer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The PlainRequestor class starts with a requestToInstantiate method, which takes two parameters: a url string and a
 * goalClass class type. The url string specifies the URL to send the HTTP request to, and the goalClass type specifies
 * the Java class to deserialize the response into.
 *
 * Inside the requestToInstantiate method, a new URL object is created using the url string.
 * Then, an HttpURLConnection object is created to send the request to the URL.
 * The clientConnection.connect() method is called to actually send the request and connect to the remote server.
 *
 * A response variable is initialized to null, which will hold the deserialized response object if the request is
 * successful.
 *
 * If the HTTP response code is 200 (i.e. the request was successful), the Moshi library is used to deserialize the
 * response into a Java object of the type specified by goalClass. This is done by creating a new Moshi object and
 * calling its adapter method to obtain a JsonAdapter for the goalClass, and then calling its fromJson method to
 * deserialize the response data into a new object of the specified type.
 *
 * The clientConnection.disconnect() method is called to close the connection to the remote server.
 * Finally, the response object is returned from the method.
 */
public class PlainRequester implements Requestor {

    public <T> T requestToInstantiate(String url, Class<T> goalClass) throws IOException {
        URL urlRequest = new URL(url);
        HttpURLConnection clientConnection = (HttpURLConnection) urlRequest.openConnection();
        clientConnection.connect();
        T response = null;
        // if the request is successful
        if (clientConnection.getResponseCode() == 200) {
            Moshi moshi = new Moshi.Builder().build();
            response =
                    moshi
                            .adapter(goalClass)
                            .fromJson(new Buffer().readFrom(clientConnection.getInputStream()));
        }
        clientConnection.disconnect();
        return response;
    }
}
