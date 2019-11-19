import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class WebServer implements BuildService<WebServer.Params>, AutoCloseable {
    interface Params extends BuildServiceParameters {
        Property<Integer> getPort();

        DirectoryProperty getResources();
    }

    public WebServer() throws URISyntaxException {
        int port = getParameters().getPort().get();
        URI uri = new URI(String.format("https://localhost:%d/", port));

        // start the server ...

        System.out.println(String.format("Server is running at %s", uri));
    }

    @Override
    public void close() {
        // stop the server ...
    }
}
