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

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SupplierUtils {
    private SupplierUtils() {}

    public static <T> Supplier<T> memoize(@NotNull Supplier<T> supplier) {
        return new MemoizingSupplier<>(supplier);
    }

    public static final class MemoizingSupplier<T> implements Supplier<T> {
        private volatile T value;
        private Supplier<T> delegate;

        public MemoizingSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            T v = value;
            if (v == null) {
                synchronized (this) {
                    v = value;
                    if (v == null) {
                        v = delegate.get();
                        value = v;
                        delegate = null; // allow GC
                    }
                }
            }
            return v;
        }
    }
}
