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
package com.thoughtworks.go.junit5;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class JsonSourceProvider implements ArgumentsProvider, AnnotationConsumer<JsonSource> {
    private String[] jsonFiles;

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        Object[] files = Arrays.stream(jsonFiles)
                .filter(Objects::nonNull)
                .map(this::readFile)
                .toArray();

        return Stream.of(Arguments.of(files));
    }

    private String readFile(String file) {
        try {
            return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(JsonSource jsonSource) {
        jsonFiles = jsonSource.jsonFiles();
    }
}