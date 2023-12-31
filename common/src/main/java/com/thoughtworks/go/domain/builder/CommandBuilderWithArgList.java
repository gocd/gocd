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
package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.domain.RunIfConfigs;

import java.io.File;

public class CommandBuilderWithArgList extends BaseCommandBuilder {

    private String[] args;

    public CommandBuilderWithArgList(String command, String[] args, File workingDir, RunIfConfigs conditions,
                                     Builder cancelBuilder, String description) {
        super(conditions, cancelBuilder, description, command, workingDir);
        this.args = args;
    }

    @Override
    protected String[] argList() {
        return args;
    }

}
