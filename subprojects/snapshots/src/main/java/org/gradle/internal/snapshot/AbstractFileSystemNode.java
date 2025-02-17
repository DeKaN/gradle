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

package org.gradle.internal.snapshot;

import java.util.Optional;

public abstract class AbstractFileSystemNode implements FileSystemNode {

    private final String pathToParent;

    public AbstractFileSystemNode(String pathToParent) {
        this.pathToParent = pathToParent;
    }

    /**
     * Returns metadata about this node if available.
     */
    protected abstract Optional<MetadataSnapshot> getMetadata();

    protected abstract Optional<MetadataSnapshot> getChildMetadata(String absolutePath, int offset, CaseSensitivity caseSensitivity);

    @Override
    public String getPathToParent() {
        return pathToParent;
    }

    @Override
    public Optional<MetadataSnapshot> getSnapshot(String absolutePath, int offset, CaseSensitivity caseSensitivity) {
        return SnapshotUtil.thisOrGet(
            getMetadata().orElse(null),
            absolutePath, offset,
            () -> getChildMetadata(absolutePath, offset, caseSensitivity)
        );
    }
}
