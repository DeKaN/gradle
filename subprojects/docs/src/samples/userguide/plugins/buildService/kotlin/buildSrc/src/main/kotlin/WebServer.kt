import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class WebServer : BuildService<WebServer.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        val port: Property<Int>
        val resources: DirectoryProperty
    }

    init {
        val port = parameters.port.get()
        val uri = URI("https://localhost:$port/")

        // start the server

        println("Server is running at $uri")
    }

    fun close() {
        // stop the server
    }
}
