import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.provider.Property
import org.gradle.api.file.DirectoryProperty
import java.net.URI

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

    override fun close() {
        // stop the server
    }
}
