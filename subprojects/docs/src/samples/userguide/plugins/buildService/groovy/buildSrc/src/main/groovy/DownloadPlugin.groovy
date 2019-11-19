import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class DownloadPlugin implements Plugin<Project> {
    void apply(Project project) {
        // Register the service
        def serviceProvider = project.gradle.sharedServices.registerIfAbsent("web", WebServer) {
            parameters.port = 5005
        }

        // Connect the provider to the task
        project.tasks.register("download", Download) {
            server = serviceProvider
            outputFile = project.layout.buildDirectory.file('result.zip')
        }
    }
}
