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

/**
 * Models a type of primitive optional boolean value that can be true, false, or unset/unknown. May be better to just
 * model everything as using <code>Optional&lt;Boolean&gt;</code> in modern Java versions; although this implementation
 * does avoid very mild boxing/unboxing checks for booleans.
 */
@FunctionalInterface
public interface TriState {

    boolean get();

    default boolean isPresent() {
        return true;
    }

    default void ifPresent(BooleanConsumer consumer) {
        consumer.accept(this.get());
    }

    TriState FALSE = () -> false;
    TriState TRUE = () -> true;
    TriState UNSET = new TriState() {
        @Override
        public boolean get() {
            throw new IllegalStateException("State is unset.");
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public void ifPresent(BooleanConsumer consumer) {} // do nothing
    };

    static TriState from(String booleanLike) {
        if (booleanLike == null || booleanLike.isBlank()) {
            return UNSET;
        }
        if (booleanLike.equalsIgnoreCase("false")) {
            return FALSE;
        }
        if (booleanLike.equalsIgnoreCase("true")) {
            return TRUE;
        }
        throw new IllegalArgumentException(String.format("The string '%s' does not look like a boolean.", booleanLike));
    }

    static TriState from(boolean booleanValue) {
        if (booleanValue) {
            return TRUE;
        } else {
            return FALSE;
        }
    }
}
