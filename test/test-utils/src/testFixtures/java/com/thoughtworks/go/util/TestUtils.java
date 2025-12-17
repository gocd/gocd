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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class TestUtils {

    private TestUtils() {}

    public static void copyAndClose(InputStream inputStream, OutputStream outputStream) {
        try (InputStream is = inputStream; OutputStream os = outputStream) {
            is.transferTo(os);
        } catch (IOException e) {
            bomb(e);
        }
    }

    public static void sleepQuietly(Duration duration) {
        doInterruptiblyQuietly(() -> Thread.sleep(duration.toMillis()));
    }

    public static void sleepQuietly(long millis) {
        doInterruptiblyQuietly(() -> Thread.sleep(millis));
    }

    public static void sleepQuietlyRethrowInterrupt(Duration duration) {
        doInterruptiblyQuietly(() -> Thread.sleep(duration.toMillis()));
    }

    public static void sleepQuietlyRethrowInterrupt(long millis) {
        doInterruptiblyQuietlyRethrowInterrupt(() -> Thread.sleep(millis));
    }

    public static void doInterruptiblyQuietly(Interruptible function) {
        try {
            doInterruptiblyQuietlyRethrowInterrupt(function);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            } else {
                throw e;
            }
        }
    }

    public static void doInterruptiblyQuietlyRethrowInterrupt(Interruptible function) {
        try {
            function.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface Interruptible {
        void run() throws Exception;
    }

}
