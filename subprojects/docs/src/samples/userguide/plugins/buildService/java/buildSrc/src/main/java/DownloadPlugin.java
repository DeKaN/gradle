import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.provider.Provider;

public class DownloadPlugin implements Plugin<Project> {
    public void apply(Project project) {
        // Register the service
        Provider<WebServer> serviceProvider = project.getGradle().getSharedServices().registerIfAbsent("web", WebServer.class, spec -> {
            spec.getParameters().getPort().set(5005);
        });

        // Connect the provider to the task
        project.getTasks().register("download", Download.java, task -> {
            task.getServer().set(serviceProvider);
        });
    }
}
