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
package com.thoughtworks.go.server.cache;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CacheKeyGenerator {
    private static final String DELIMITER = ".$";
    private final Class<?> clazz;

    public CacheKeyGenerator(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String generate(String identifier, Object... args) {
        final List<Object> allArgs = Arrays.stream(args).map(arg -> {
            if (isAllowed(arg)) {
                return arg;
            }
            throw new IllegalArgumentException("Type " + arg.getClass() + " is not allowed here!");
        }).map(arg -> {
            if (arg instanceof CaseInsensitiveString) {
                return ((CaseInsensitiveString) arg).toLower();
            } else {
                return arg;
            }
        }).collect(Collectors.toList());

        allArgs.add(0, clazz.getName());
        allArgs.add(1, identifier);

        return StringUtils.join(allArgs, DELIMITER).intern();
    }

    private static boolean isAllowed(Object arg) {
        return arg == null || arg instanceof String || arg instanceof CaseInsensitiveString ||
                arg instanceof Number || arg instanceof Boolean || arg instanceof Enum;
    }
}
