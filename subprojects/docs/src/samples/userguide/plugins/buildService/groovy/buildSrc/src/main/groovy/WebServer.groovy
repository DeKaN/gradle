import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class WebServer implements BuildService<Params>, AutoCloseable {
    interface Params extends BuildServiceParameters {
        Property<Int> getPort()

        DirectoryProperty getResources()
    }

    WebServer() {
        def port = parameters.port.get()
        def uri = new URI("https://localhost:$port/")

        // start the server ...

        System.out.println("Server is running at $url")
    }

    void close() {
        // stop the server ...
    }
}
