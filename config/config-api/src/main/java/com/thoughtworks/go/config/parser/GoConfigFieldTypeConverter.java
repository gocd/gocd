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
package com.thoughtworks.go.config.parser;

import java.io.File;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.propertyeditors.FileEditor;
import org.apache.commons.lang3.StringUtils;

public class GoConfigFieldTypeConverter extends SimpleTypeConverter {

    private static final CustomizedFileEditor propertyEditor = new CustomizedFileEditor();

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
