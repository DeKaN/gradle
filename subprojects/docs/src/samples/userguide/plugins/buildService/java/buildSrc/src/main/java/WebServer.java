import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

public abstract class WebServer implements BuildService<WebServer.Params>, AutoCloseable {
    interface Params extends BuildServiceParameters {
        Property<Int> getPort();
        DirectoryProperty getResources();
    }

    public WebServer() {
        int port = getParameters().getPort().get();
        URI uri = new URI(String.format("https://localhost:%d/", port));

        // start the server ...

        System.out.println(String.format("Server is running at %s", uri));
    }

    void close() {
        // stop the server ...
    }
}
