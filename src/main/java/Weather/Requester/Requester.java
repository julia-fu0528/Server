package Weather.Requester;

import java.io.IOException;

public interface Requester {
    /**
     * retrieves information from Json and deserializes after retrieving web information from url
     * @param url the String to enter in the browser
     * @param goalClass the generic class to deserialize Json into
     * @return generic class T
     * @param <T> generic
     * @throws IOException
     */
    public <T> T requestToInstantiate(String url, Class<T> goalClass) throws IOException;

}
