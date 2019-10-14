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

package org.gradle.internal.vfs.impl;

import org.gradle.internal.snapshot.FileSystemLocationSnapshot;

import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Predicate;

public interface Node {
    @Nullable
    Node getChild(String name);
    Node getOrCreateChild(String name, ChildNodeSupplier nodeSupplier);
    Node replaceChild(String name, ChildNodeSupplier nodeSupplier, ExistingChildPredicate shouldReplaceExisting);
    void removeChild(String name);
    String getAbsolutePath();
    Type getType();

    FileSystemLocationSnapshot getSnapshot();

    default String getChildAbsolutePath(String name) {
        return getAbsolutePath() + File.separatorChar + name;
    }

    enum Type {
        FILE,
        DIRECTORY,
        MISSING,
        UNKNOWN
    }

    interface ChildNodeSupplier {
        Node create(Node parent);
    }

    interface ExistingChildPredicate extends Predicate<Node> {
        @Override
        boolean test(Node existingChild);
    }
}
