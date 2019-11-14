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

package org.gradle.internal.snapshot

import org.spockframework.mock.EmptyOrDummyResponse
import org.spockframework.mock.IDefaultResponse
import org.spockframework.mock.IMockInvocation
import spock.lang.Specification

import static org.gradle.internal.snapshot.CaseSensitivity.CASE_SENSITIVE

class UnknownSnapshotTest extends Specification {
    def "returns empty when queried at root"() {
        def node = new UnknownSnapshot("some/prefix", createChildren("myFile.txt"))

        when:
        def snapshot = node.getSnapshot(OffsetRelativePath.of("/absolute/some/prefix", "/absolute/some/prefix".length() + 1), CASE_SENSITIVE)
        then:
        !snapshot.present
        0 * _
    }

    def "queries child when queried for path in child"() {
        given:
        def children = createChildren(childNames)
        def first = children.get(0)
        def node = new UnknownSnapshot("some/prefix", children)
        def relativePath = "${first.pathToParent}/someString"
        def result = Mock(MetadataSnapshot)

        when:
        def snapshot = node.getSnapshot(OffsetRelativePath.of(relativePath, 0), CASE_SENSITIVE)
        then:
        snapshot.get() == result
        _ * first.getSnapshot(OffsetRelativePath.of(relativePath, first.pathToParent.length() + 1), CASE_SENSITIVE) >> Optional.of(result)

        where:
        childNames << [["first", "second", "third"]]
    }

    def "finds no snapshot when no child has a similar pathToParent"() {
        given:
        def children = createChildren(childNames)
        def first = children.get(0)
        def node = new UnknownSnapshot("some/prefix", children)
        def relativePath = "${first.pathToParent}1/someString"

        when:
        def snapshot = node.getSnapshot(OffsetRelativePath.of(relativePath, 0), CASE_SENSITIVE)
        then:
        !snapshot.present
        0 * _.getSnapshot(_)

        where:
        childNames << [["first"], ["first", "second"], ["first", "second", "third"]]
    }

    def "invalidating unknown child does nothing"() {
        given:
        def children = createChildren(childNames)
        def node = new UnknownSnapshot("some/prefix", children)
        def relativePath = "first/outside"

        when:
        def result = node.invalidate(OffsetRelativePath.of(relativePath, 0), CASE_SENSITIVE)
        then:
        0 * _.invalidate(_)
        result.get() == node

        where:
        childNames << [["first/within"], ["first/within", "second"], ["first/within", "second", "third"]]
    }

    def "invalidating only child returns empty"() {
        given:
        def children = createChildren("first")
        def node = new UnknownSnapshot("some/prefix", children)
        def childToInvalidate = children.get(0)
        def relativePath = childToInvalidate.pathToParent

        when:
        def result = node.invalidate(OffsetRelativePath.of(relativePath, 0), CASE_SENSITIVE)
        then:
        0 * _.invalidate(_)
        !result.present
    }

    def "invalidating known child removes it"() {
        given:
        def children = createChildren(childNames)
        def node = new UnknownSnapshot("some/prefix", children)
        def childToInvalidate = children.get(0)
        def relativePath = childToInvalidate.pathToParent
        def snapshot = Mock(MetadataSnapshot)
        def remainingChildren = children.findAll { it != childToInvalidate }

        when:
        def result = node.invalidate(OffsetRelativePath.of(relativePath, 0), CASE_SENSITIVE).get()
        remainingChildren.each {
            assert result.getSnapshot(OffsetRelativePath.of(it.pathToParent, 0), CASE_SENSITIVE).get() == snapshot
        }
        then:
        0 * _.invalidate(_)
        interaction {
            remainingChildren.each {
                _ * it.getSnapshot(OffsetRelativePath.of(it.pathToParent, it.pathToParent.length() + 1), CASE_SENSITIVE) >> Optional.of(snapshot)
            }
        }

        when:
        def removedSnapshot = result.getSnapshot(OffsetRelativePath.of(childToInvalidate.pathToParent, 0), CASE_SENSITIVE)
        then:
        0 * _.getSnapshot(_)
        !removedSnapshot.present

        where:
        childNames << [["first", "fourth"], ["first", "second"], ["first", "second", "third"], ["first", "second", "third", "fourth"]]
    }

    def "invalidating location within child works"() {
        given:
        def children = createChildren(childNames)
        def node = new UnknownSnapshot("some/prefix", children)
        def childWithChildToInvalidate = children.get(0)
        def invalidatedChild = Mock(FileSystemNode, defaultResponse: new RespondWithPathToParent(childWithChildToInvalidate.pathToParent))

        when:
        def result = node.invalidate(OffsetRelativePath.of("${childWithChildToInvalidate.pathToParent}/deeper", 0), CASE_SENSITIVE).get()

        then:
        1 * childWithChildToInvalidate.invalidate(OffsetRelativePath.of("${childWithChildToInvalidate.pathToParent}/deeper", childWithChildToInvalidate.pathToParent.length() + 1), CASE_SENSITIVE) >> Optional.of(invalidatedChild)
        0 * _.invalidate(_)
        !result.getSnapshot(OffsetRelativePath.of(invalidatedChild.pathToParent, 0), CASE_SENSITIVE).present


        where:
        childNames << [["first/more"], ["first/some", "second/other"], ["first/even/deeper", "second", "third/whatever"], ["first/more/stuff", "second", "third", "fourth"]]
    }

    private List<FileSystemNode> createChildren(String... pathsToParent) {
        createChildren(pathsToParent as List)
    }

    private List<FileSystemNode> createChildren(Iterable<String> pathsToParent) {
        pathsToParent.sort()
        List<FileSystemNode> result = []
        pathsToParent.each {
            result.add(Mock(FileSystemNode, defaultResponse: new RespondWithPathToParent(it)))
        }
        return result
    }

    private static class RespondWithPathToParent implements IDefaultResponse {
        private final String pathToParent

        RespondWithPathToParent(String pathToParent) {
            this.pathToParent = pathToParent
        }

        @Override
        Object respond(IMockInvocation invocation) {
            if (invocation.getMethod().name == "getPathToParent") {
                return pathToParent
            }
            return EmptyOrDummyResponse.INSTANCE.respond(invocation)
        }
    }
}
