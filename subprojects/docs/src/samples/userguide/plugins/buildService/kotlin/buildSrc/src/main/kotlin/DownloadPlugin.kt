import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.provider.Provider

class DownloadPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Register the service
        val serviceProvider = project.gradle.sharedServices.registerIfAbsent("web", WebServer::class.java) {
            it.parameters.port.set(5005)
        }

        // Connect the provider to the task
        project.tasks.register("download", Download::class.java) {
            it.server.set(serviceProvider)
            it.outputFile.set(project.layout.buildDirectory.file("result.zip"))
        }
    }
}
