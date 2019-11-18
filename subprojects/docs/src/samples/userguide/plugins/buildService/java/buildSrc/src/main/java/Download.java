import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.file.RegularFileProperty;

public abstract class Download extends DefaultTask {
    @Internal
    abstract Property<WebServer> getServer();

    @OutputFile
    abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void download() {
        WebServer server = getServer().get();
        // use the server to download a file
    }
}
