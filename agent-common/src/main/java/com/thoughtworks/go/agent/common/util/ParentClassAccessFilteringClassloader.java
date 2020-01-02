/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.agent.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This classloader allows loading of classes from parent classloader ONLY for classes mentioned in permitted-classes list
 * if given empty list of classes, it will allow loading of any class
 */
public final class ParentClassAccessFilteringClassloader extends ClassLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ParentClassAccessFilteringClassloader.class);

    private final Class[] permittedParentDefns;

    public ParentClassAccessFilteringClassloader(ClassLoader parent, Class... permittedParentDefns) {
        super(parent);
        this.permittedParentDefns = permittedParentDefns;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (permittedParentDefns.length == 0) {
            return super.loadClass(name, false);
        }
        for (Class permittedParentDefn : permittedParentDefns) {
            if (permittedParentDefn.getCanonicalName().equals(name)) {
                ClassLoader parent = getParent();
                LOG.info("Loading {} using {}", name, parent == null ? "null classloader" : parent.getClass().getCanonicalName());
                return super.loadClass(name, false);
            }
        }
        return getSystemClassLoader().loadClass(name);
    }
}
