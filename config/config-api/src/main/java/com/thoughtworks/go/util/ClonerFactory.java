/*
 * Copyright Thoughtworks, Inc.
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
import java.util.Map;

/**
 * Provides a preconfigured {@link Cloner} instance for deep cloning objects.
 * <p>
 * This predominantly exists to provide cloners for classes that are not natively supported by the {@link Cloner} library
 * and might otherwise work incorrectly, or need to access internals of JDK types unnecessarily which is generally
 * disallowed from Java 15+ and requires --add-opens to be used.
 */
public class ClonerFactory {

    private static class Builder {

        // Lazy-init, thread-safe singleton
        private static final Cloner INSTANCE = create(new Cloner());

        private static Cloner create(final Cloner cloner) {
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
