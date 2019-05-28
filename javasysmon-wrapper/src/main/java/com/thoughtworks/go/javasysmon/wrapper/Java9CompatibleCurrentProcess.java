/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.javasysmon.wrapper;

import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Java9CompatibleCurrentProcess implements CurrentProcess {

    @Override
    public void infanticide() {
        LOG.debug("Using java 9 compatible infanticide");
        try {
            MethodUtils.invokeMethod(currentProcess(), "descendants");
            Stream<Object> processHandles = (Stream<Object>) MethodUtils.invokeMethod(currentProcess(), "descendants");
            processHandles.forEach(processHandle -> {
                ProcessHandleReflectionDelegate ph = new ProcessHandleReflectionDelegate(processHandle);
                LOG.debug("Attempting to destroy process {}", ph);
                if (ph.isAlive()) {
                    ph.destroy();
                    LOG.debug("Destroyed process {}", ph);
                }
            });
        } catch (Exception e) {
            LOG.error("Unable to infanticide", e);
        }
    }

    private Object currentProcess() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class<?> handle = Class.forName("java.lang.ProcessHandle");
        return handle.getMethod("current").invoke(null);
    }

    @Override
    public long currentPid() {
        try {
            return (long) invokeMethod(currentProcess(), "pid");
        } catch (Exception e) {
            // ideally we never reach here.
            LOG.error("Unable to determine current pid", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Object> immediateChildren() {
        try {
            return ((Stream<Object>) invokeMethod(currentProcess(), "children")).collect(Collectors.toList());
        } catch (Exception e) {
            LOG.warn("Unable to find immediate children of current process", e);
            throw new RuntimeException(e);
        }
    }

    private static <T> T invokeMethod(Object o, String methodName) {
        try {
            return (T) MethodUtils.invokeMethod(o, methodName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // needed because ProcessHandle class is unavailable on java 8, so we hack around!
    class ProcessHandleReflectionDelegate {
        final Object delegate;

        ProcessHandleReflectionDelegate(Object delegate) {
            this.delegate = delegate;
        }

        public boolean isAlive() {
            try {
                return (boolean) invokeMethod(this.delegate, "isAlive");
            } catch (Exception e) {
                LOG.warn("Ignoring error", e);
            }
            return false;
        }

        public void destroy() {
            try {
                invokeMethod(this.delegate, "destroy");
            } catch (Exception e) {
                LOG.warn("Ignoring error", e);
            }
        }

        @Override
        public String toString() {
            try {
                return "pid(" + this.delegate + ") " + invokeMethod(this.delegate, "info");
            } catch (Exception e) {
                LOG.warn("Ignoring error", e);
            }
            return "Unable to #toString";
        }
    }

}
