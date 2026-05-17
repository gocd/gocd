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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SupplierUtilsTest {

    @Test
    void memoizeSupplierShouldCacheResults() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = SupplierUtils.memoize(counter::incrementAndGet);

        assertEquals(1, supplier.get());
        assertEquals(1, supplier.get());
        assertEquals(1, supplier.get());
        assertEquals(2, counter.incrementAndGet());
    }

    @Test
    void rethrowSupplierShouldReturnNormally() {
        Supplier<String> supplier = SupplierUtils.rethrow(() -> "result");
        assertThat(supplier).returns("result", Supplier::get);
    }

    @Test
    void rethrowSupplierShouldCatchExceptions() {
        Supplier<String> supplier = SupplierUtils.rethrow(() -> {
            throw new IOException("IO error");
        });
        assertThatThrownBy(supplier::get)
           .isExactlyInstanceOf(RuntimeException.class)
           .hasRootCauseExactlyInstanceOf(IOException.class);
    }

    @Test
    void rethrowSupplierShouldRethrowRuntimeExceptionsAsIs() {
        Supplier<String> supplier = SupplierUtils.rethrow(() -> {
            throw new IllegalArgumentException("bad argument");
        });
        assertThatThrownBy(supplier::get)
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasNoCause();
    }
}