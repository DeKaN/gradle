import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class Download extends DefaultTask {
    @Internal
    abstract Property<WebServer> getServer()

    @OutputFile
    abstract RegularFileProperty getOutputFile()

    @TaskAction
    void download() {
        def server = server.get()
        // use the server to download a file
    }
}
