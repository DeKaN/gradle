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

package org.gradle.instantexecution

class InstantExecutionTaskActionsIntegrationTest extends AbstractInstantExecutionIntegrationTest {

    def "task can have doFirst/doLast groovy script closures"() {

        given:
        settingsFile << """
            include 'a', 'b'
        """
        [file("a/build.gradle"), file("b/build.gradle")].each {
            it << """
                tasks.register("some") {
                    doFirst {
                        println("FIRST")
                    }
                    doLast {
                        println("LAST")
                    }
                }
                tasks.register("other") {
                    doFirst {
                        println("OTHER")
                    }
                }
            """
        }

        when:
        instantRun ":a:some"

        then:
        result.groupedOutput.task(":a:some").assertOutputContains("FIRST").assertOutputContains("LAST")

        when:
        instantRun ":a:some"

        then:
        result.groupedOutput.task(":a:some").assertOutputContains("FIRST").assertOutputContains("LAST")

        when: // rebuild task graph, with tasks from the same set of scripts
        instantRun ":a:some", ":a:other"

        then:
        result.groupedOutput.task(":a:some").assertOutputContains("FIRST").assertOutputContains("LAST")
        result.groupedOutput.task(":a:other").assertOutputContains("OTHER")

        when:
        instantRun ":a:some", ":a:other"

        then:
        result.groupedOutput.task(":a:some").assertOutputContains("FIRST").assertOutputContains("LAST")
        result.groupedOutput.task(":a:other").assertOutputContains("OTHER")

        when: // rebuild task graph, with tasks from a different set of scripts
        instantRun ":b:some"

        then:
        result.groupedOutput.task(":b:some").assertOutputContains("FIRST").assertOutputContains("LAST")

        when:
        instantRun ":b:some"

        then:
        result.groupedOutput.task(":b:some").assertOutputContains("FIRST").assertOutputContains("LAST")
    }

    def "task can have doFirst/doLast anonymous script actions"() {

        given:
        buildFile << """
            tasks.register("some") {
                doFirst(new Action<Task>() {
                    void execute(task) {
                        println("FIRST")
                    }
                })
                doLast(new Action<Task>() {
                    void execute(task) {
                        println("LAST")
                    }
                })
            }
            tasks.register("other") {
                doFirst(new Action<Task>() {
                    void execute(task) {
                        println("OTHER")
                    }
                })
            }
        """

        when:
        instantRun "some"

        then:
        result.groupedOutput.task(":some").assertOutputContains("FIRST").assertOutputContains("LAST")

        when:
        instantRun "some"

        then:
        result.groupedOutput.task(":some").assertOutputContains("FIRST").assertOutputContains("LAST")

        when:
        instantRun "some", "other"

        then:
        result.groupedOutput.task(":some").assertOutputContains("FIRST").assertOutputContains("LAST")
        result.groupedOutput.task(":other").assertOutputContains("OTHER")

        when:
        instantRun "some", "other"

        then:
        result.groupedOutput.task(":some").assertOutputContains("FIRST").assertOutputContains("LAST")
        result.groupedOutput.task(":other").assertOutputContains("OTHER")
    }

    def "task can have doFirst/doLast kotlin script lambdas"() {

        given:
        settingsFile << """
            include 'a', 'b'
        """
        [file("a/build.gradle.kts"), file("b/build.gradle.kts")].each {
            it << """
                tasks.register("some") {
                    doFirst {
                        println("FIRST")
                    }
                    doLast {
                        println("LAST")
                    }
                }
    
                tasks.register("other") {
                    doFirst {
                        println("OTHER")
                    }
                }
            """
        }

        when:
        instantRun ":a:some"

        then:
        result.groupedOutput.task(":a:some").assertOutputContains("FIRST").assertOutputContains("LAST")

        when:
        instantRun ":a:some"

        then:
        result.groupedOutput.task(":a:some").assertOutputContains("FIRST").assertOutputContains("LAST")

        when: // rebuild task graph, with tasks from the same set of scripts
        instantRun ":a:some", ":a:other"

        then:
        result.groupedOutput.task(":a:some").assertOutputContains("FIRST").assertOutputContains("LAST")
        result.groupedOutput.task(":a:other").assertOutputContains("OTHER")

        when:
        instantRun ":a:some", ":a:other"

        then:
        result.groupedOutput.task(":a:some").assertOutputContains("FIRST").assertOutputContains("LAST")
        result.groupedOutput.task(":a:other").assertOutputContains("OTHER")

        when: // rebuild task graph, with tasks from a different set of scripts
        instantRun ":b:some"

        then:
        result.groupedOutput.task(":b:some").assertOutputContains("FIRST").assertOutputContains("LAST")

        when:
        instantRun ":b:some"

        then:
        result.groupedOutput.task(":b:some").assertOutputContains("FIRST").assertOutputContains("LAST")
    }

    def "name conflicts of types declared in groovy scripts"() {

        given:
        settingsFile << """include("a", "b")"""
        file("a/build.gradle") << """

            tasks.register("some") {
                doLast { println("A") }
            }
        """
        file("b/build.gradle") << """

            tasks.register("some") {
                doLast { println("B") }
            }
        """

        when:
        instantRun "some"

        then:
        outputContains("A")
        outputContains("B")

        when:
        instantRun "some"

        then:
        outputContains("A")
        outputContains("B")
    }

    def "name conflicts of types declared in kotlin scripts"() {

        given:
        settingsFile << """include("a", "b")"""
        file("a/build.gradle.kts") << """

            tasks.register("some") {
                doLast { println("A") }
            }
        """
        file("b/build.gradle.kts") << """

            tasks.register("some") {
                doLast { println("B") }
            }
        """

        when:
        instantRun "some"

        then:
        outputContains("A")
        outputContains("B")

        when:
        instantRun "some"

        then:
        outputContains("A")
        outputContains("B")
    }
}
