package Weather.Requester;

import java.io.IOException;

public interface Requestor {
    public <T> T requestToInstantiate(String url, Class<T> goalClass) throws IOException;

}
