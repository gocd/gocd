/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.common;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.File;

public class FileIsReadableValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value == null || value.trim().isEmpty()) {
            throw new ParameterException(name + " must be specified.");
        }

        if (!new File(value).canRead()) {
            throw new ParameterException(name + " must be a file that is readable.");
        }
    }
}
