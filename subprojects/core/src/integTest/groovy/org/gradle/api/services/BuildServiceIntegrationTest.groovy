/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.services

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class BuildServiceIntegrationTest extends AbstractIntegrationSpec {
    def "service is created once per build on first use and stopped at the end of the build"() {
        serviceImplementation()
        buildFile << """
            def provider = gradle.sharedServices.maybeRegister("counter", CountingService) {
                parameters.initial = 10
            }
            
            task first {
                doFirst {
                    provider.get().increment()
                }
            }

            task second {
                doFirst {
                    provider.get().increment()
                }
            }
        """

        when:
        run("first", "second")

        then:
        output.count("service:") == 4
        outputContains("service: created with value = 10")
        outputContains("service: value is 11")
        outputContains("service: value is 12")
        outputContains("service: closed with value 12")

        when:
        run("first", "second")

        then:
        output.count("service:") == 4
        outputContains("service: created with value = 10")
        outputContains("service: value is 11")
        outputContains("service: value is 12")
        outputContains("service: closed with value 12")

        when:
        run("help")

        then:
        result.assertNotOutput("service:")
    }

    def "service can be used at configuration time"() {
        serviceImplementation()
        buildFile << """
            def provider = gradle.sharedServices.maybeRegister("counter", CountingService) {
                parameters.initial = 10
            }
            
            task count {
                doFirst {
                    provider.get().increment()
                }
            }

            provider.get().increment()
        """

        when:
        run("count")

        then:
        output.count("service:") == 4
        outputContains("service: created with value = 10")
        outputContains("service: value is 11")
        outputContains("service: value is 12")
        outputContains("service: closed with value 12")

        when:
        run("count")

        then:
        output.count("service:") == 4
        outputContains("service: created with value = 10")
        outputContains("service: value is 11")
        outputContains("service: value is 12")
        outputContains("service: closed with value 12")

        when:
        run("help")

        then:
        output.count("service:") == 3
        outputContains("service: created with value = 10")
        outputContains("service: value is 11")
        outputContains("service: closed with value 11")
    }

    def "plugin applied to multiple projects can register a shared service"() {
        settingsFile << "include 'a', 'b', 'c'"
        serviceImplementation()
        buildFile << """
            class CounterPlugin implements Plugin<Project> {
                void apply(Project project) {
                    def provider = project.gradle.sharedServices.maybeRegister("counter", CountingService) {
                        parameters.initial = 10
                    }
                    project.tasks.register("count") {
                        doFirst {
                            provider.get().increment()
                        }
                    }                
                }
            }
            subprojects {
                apply plugin: CounterPlugin
            }
        """

        when:
        run("count")

        then:
        output.count("service:") == 5
        outputContains("service: created with value = 10")
        outputContains("service: value is 11")
        outputContains("service: value is 12")
        outputContains("service: value is 13")
        outputContains("service: closed with value 13")

        when:
        run("count")

        then:
        output.count("service:") == 5
        outputContains("service: created with value = 10")
        outputContains("service: value is 11")
        outputContains("service: value is 12")
        outputContains("service: value is 13")
        outputContains("service: closed with value 13")
    }

    def "plugin can apply conventions to shared services of a given type"() {
        serviceImplementation()
        buildFile << """
            class CounterConventionPlugin implements Plugin<Project> {
                void apply(Project project) {
                    project.gradle.sharedServices.registrations.configureEach {
                        if (parameters instanceof Params) {
                            parameters.initial = parameters.initial.get() + 5
                        }
                    }
                }
            }
            apply plugin: CounterConventionPlugin

            def counter1 = project.gradle.sharedServices.maybeRegister("counter1", CountingService) {
                parameters.initial = 0
            }
            def counter2 = project.gradle.sharedServices.maybeRegister("counter2", CountingService) {
                parameters.initial = 10
            }
            task count {
                doLast {
                    counter1.get().increment()
                    counter2.get().increment()
                }
            }
        """

        when:
        run("count")

        then:
        output.count("service:") == 6
        outputContains("service: created with value = 5")
        outputContains("service: created with value = 15")
        outputContains("service: value is 6")
        outputContains("service: value is 16")
        outputContains("service: closed with value 6")
        outputContains("service: closed with value 16")

        when:
        run("count")

        then:
        output.count("service:") == 6
        outputContains("service: created with value = 5")
        outputContains("service: created with value = 15")
        outputContains("service: value is 6")
        outputContains("service: value is 16")
        outputContains("service: closed with value 6")
        outputContains("service: closed with value 16")
    }

    def "service is stopped even if build fails"() {
        serviceImplementation()
        buildFile << """
            def counter1 = project.gradle.sharedServices.maybeRegister("counter1", CountingService) {
                parameters.initial = 0
            }
            def counter2 = project.gradle.sharedServices.maybeRegister("counter2", CountingService) {
                parameters.initial = 10
            }
            task count {
                doLast {
                    counter1.get().increment()
                    throw new RuntimeException("broken")
                }
            }
        """

        when:
        fails("count")

        then:
        output.count("service:") == 3
        outputContains("service: created with value = 0")
        outputContains("service: value is 1")
        outputContains("service: closed with value 1")

        when:
        fails("count")

        then:
        output.count("service:") == 3
        outputContains("service: created with value = 0")
        outputContains("service: value is 1")
        outputContains("service: closed with value 1")
    }

    def "reports failure to create the service instance"() {
        brokenServiceImplementation()
        buildFile << """
            def provider1 = gradle.sharedServices.maybeRegister("counter1", CountingService) {
                parameters.initial = 10
            }
            def provider2 = gradle.sharedServices.maybeRegister("counter2", CountingService) {
                parameters.initial = 10
            }
            
            task first {
                doFirst {
                    provider1.get().increment()
                }
            }

            task second {
                doFirst {
                    provider2.get().increment()
                }
            }
        """

        when:
        fails("first", "second", "--continue")

        then:
        failure.assertHasFailures(2)
        failure.assertHasDescription("Execution failed for task ':first'.")
        failure.assertHasCause("Failed to create service 'counter1'.")
        failure.assertHasCause("Could not create an instance of type CountingService.")
        failure.assertHasCause("broken")
        failure.assertHasDescription("Execution failed for task ':second'.")
        failure.assertHasCause("Failed to create service 'counter2'.")
        failure.assertHasCause("Could not create an instance of type CountingService.")
        failure.assertHasCause("broken")

        when:
        fails("first", "second", "--continue")

        then:
        failure.assertHasFailures(2)
        failure.assertHasDescription("Execution failed for task ':first'.")
        failure.assertHasCause("Failed to create service 'counter1'.")
        failure.assertHasCause("Could not create an instance of type CountingService.")
        failure.assertHasCause("broken")
        failure.assertHasDescription("Execution failed for task ':second'.")
        failure.assertHasCause("Failed to create service 'counter2'.")
        failure.assertHasCause("Could not create an instance of type CountingService.")
        failure.assertHasCause("broken")
    }

    def "reports failure to stop the service instance"() {
        brokenStopServiceImplementation()
        buildFile << """
            def provider1 = gradle.sharedServices.maybeRegister("counter1", CountingService) {
                parameters.initial = 10
            }
            def provider2 = gradle.sharedServices.maybeRegister("counter2", CountingService) {
                parameters.initial = 10
            }
            
            task first {
                doFirst {
                    provider1.get().increment()
                }
            }

            task second {
                doFirst {
                    provider2.get().increment()
                }
            }
        """

        when:
        fails("first", "second")

        then:
        // TODO - improve the error handling so as to report both failures as top level failures
        // This documents existing behaviour, not desired behaviour
        failure.assertHasDescription("Failed to notify build listener")
        failure.assertHasCause("Failed to stop service 'counter1'.")
        failure.assertHasCause("broken")
        failure.assertHasCause("Failed to stop service 'counter2'.")
        failure.assertHasCause("broken")
    }

    def serviceImplementation() {
        buildFile << """
            interface Params extends BuildServiceParameters {
                Property<Integer> getInitial()
            }
            
            abstract class CountingService implements BuildService<Params>, AutoCloseable {
                int value
                
                CountingService() {
                    value = parameters.initial.get()
                    println("service: created with value = \${value}")
                }
                
                void increment() {
                    value++
                    println("service: value is \${value}")
                }
                
                void close() {
                    println("service: closed with value \${value}")
                }
            } 
        """
    }

    def brokenServiceImplementation() {
        buildFile << """
            interface Params extends BuildServiceParameters {
                Property<Integer> getInitial()
            }
            
            abstract class CountingService implements BuildService<Params> {
                CountingService() {
                    throw new IOException("broken") // use a checked exception
                }
                
                void increment() {
                    throw new IOException("broken") // use a checked exception
                }
            } 
        """
    }

    def brokenStopServiceImplementation() {
        buildFile << """
            interface Params extends BuildServiceParameters {
                Property<Integer> getInitial()
            }
            
            abstract class CountingService implements BuildService<Params>, AutoCloseable {
                CountingService() {
                }
                
                void increment() {
                }
                
                void close() {
                    throw new IOException("broken") // use a checked exception
                }
            } 
        """
    }
}
