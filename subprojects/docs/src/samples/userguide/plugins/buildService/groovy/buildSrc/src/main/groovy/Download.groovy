import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.file.RegularFileProperty

abstract class Download extends DefaultTask {
    @Internal
    abstract Property<WebServer> getServer()

    @OutputFile
    abstract RegularFileProperty getOutputFile()

    @TaskAction
    void download() {
        WebServer server = server.get()
        // use the server to download a file
    }
}
