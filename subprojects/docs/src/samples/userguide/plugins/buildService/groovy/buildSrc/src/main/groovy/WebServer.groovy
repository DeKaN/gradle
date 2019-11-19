import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.provider.Property
import org.gradle.api.file.DirectoryProperty

abstract class WebServer implements BuildService<Params>, AutoCloseable {
    interface Params extends BuildServiceParameters {
        Property<Integer> getPort()

        DirectoryProperty getResources()
    }

    WebServer() {
        def port = parameters.port.get()
        def uri = new URI("https://localhost:$port/")

        // start the server ...

        System.out.println("Server is running at $uri")
    }

    @Override
    void close() {
        // stop the server ...
    }
}
