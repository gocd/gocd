/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.config.parser;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.propertyeditors.FileEditor;

import java.io.File;

public class GoConfigFieldTypeConverter extends SimpleTypeConverter {

    public static TypeConverter forThread() {
        return typeConverter.get();
    }

    private static final CustomizedFileEditor propertyEditor = new CustomizedFileEditor();

    // Type converters are NOT thread safe. Should be OK to cache them per thread as not too large and don't think
    // we have too many threads that will be doing so?
    private static final ThreadLocal<TypeConverter> typeConverter = ThreadLocal.withInitial(GoConfigFieldTypeConverter::new);

    public GoConfigFieldTypeConverter() {
        super();
        this.registerCustomEditor(File.class, propertyEditor);
    }

    private static class CustomizedFileEditor extends FileEditor {
        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            if (StringUtils.isBlank(text)) {
                return;
            }
            super.setAsText(text);
        }
    }
}
