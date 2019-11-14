/*
 * Copyright 2018 the original author or authors.
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

public abstract class AbstractCompleteFileSystemLocationSnapshot implements CompleteFileSystemLocationSnapshot {
    private final String absolutePath;
    private final String name;

    public AbstractCompleteFileSystemLocationSnapshot(String absolutePath, String name) {
        this.absolutePath = absolutePath;
        this.name = name;
    }

    @Override
    public String getAbsolutePath() {
        return absolutePath;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPathToParent() {
        return getName();
    }

    @Override
    public CompleteFileSystemLocationSnapshot store(OffsetRelativePath relativePath, CaseSensitivity caseSensitivity, MetadataSnapshot snapshot) {
        return this;
    }

    @Override
    public FileSystemNode withPathToParent(String newPathToParent) {
        return getPathToParent().equals(newPathToParent)
            ? this
            : new PathCompressingSnapshotWrapper(newPathToParent, this);
    }

    @Override
    public Optional<MetadataSnapshot> getSnapshot(OffsetRelativePath relativePath, CaseSensitivity caseSensitivity) {
        return SnapshotUtil.thisOrGet(this,
            relativePath,
            () -> getChildSnapshot(relativePath, caseSensitivity));
    }

    protected Optional<MetadataSnapshot> getChildSnapshot(OffsetRelativePath relativePath, CaseSensitivity caseSensitivity) {
        return Optional.of(SnapshotUtil.missingSnapshotForAbsolutePath(relativePath.getAbsolutePath()));
    }
}
