import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class DownloadPlugin implements Plugin<Project> {
    void apply(Project project) {
        // Register the service
        Provider<WebServer> serviceProvider = project.gradle.sharedServices.registerIfAbsent("web", WebServer.class) {
            parameters.port = 5005
        }

        // Connect the provider to the task
        project.tasks.register("download", Download.java) {
            server = serviceProvider
        }
    }
}
