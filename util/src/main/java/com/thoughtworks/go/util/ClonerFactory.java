/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.util;

import com.rits.cloning.Cloner;
import com.rits.cloning.IDeepCloner;

import java.io.File;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides a preconfigured {@link Cloner} instance for deep cloning objects.
 * <p>
 * <strong>NOTE</strong>: This was built to address {@link Cloner#deepClone(Object)} issues
 * introduced by JDK 15. Specifically, the specialized/optimized {@link List} and {@link Set}
 * implementations are problematic for {@link Cloner#deepClone(Object)} (ImmutableCollections.List12
 * and ImmutableCollections.Set12, to be exact) and require a custom clone implementation to work
 * properly.
 * <p>
 * Without these custom clone implementations, the cloned output of such {@link List}s and {@link Set}s
 * may break equality with their origiinals. See git commit `efdbe0c1cccbce8be9e262727dbaa8dd9cfc7130`
 * for more details.
 */
public class ClonerFactory {

    private static class Builder {

        private static final List<?> LIST_1_2 = List.of("exactly one element"); // only certain outputs of List.of()
        private static final Set<?> SET_1_2 = Set.of("exactly one element");  // only certain outputs of Set.of()

        // Lazy-init, thread-safe singleton
        private static final Cloner INSTANCE = create(new Cloner());

        private static Cloner create(final Cloner cloner) {
            cloner.registerFastCloner(LIST_1_2.getClass(), (t, _1, _2) -> List.of(((List<?>) t).toArray()));
            cloner.registerFastCloner(SET_1_2.getClass(), (t, _1, _2) -> Set.of(((Set<?>) t).toArray()));
            cloner.registerFastCloner(Date.class, (t, _1, _2) -> new Date(((Date)t).getTime()));
            cloner.registerFastCloner(java.sql.Date.class, (t, _1, _2) -> new java.sql.Date(((java.sql.Date)t).getTime()));
            cloner.registerFastCloner(Timestamp.class, ClonerFactory::cloneTimestamp);
            cloner.registerFastCloner(File.class, (t, _1, _2) -> new File(((File)t).getPath()));
            return cloner;
        }
    }

    private static Timestamp cloneTimestamp(Object t, IDeepCloner cloner, Map<Object, Object> cloners) {
        Timestamp original = (Timestamp) t;
        Timestamp clone = new Timestamp(original.getTime());
        clone.setNanos(original.getNanos());
        return clone;
    }

    public static Cloner instance() {
        return Builder.INSTANCE;
    }

    public static Cloner applyFixes(Cloner cloner) {
        return Builder.create(cloner);
    }

    private ClonerFactory() {
    }
}
