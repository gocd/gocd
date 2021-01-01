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
package com.thoughtworks.go.apiv2.materials.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.materials.Modification;

import java.util.function.Consumer;

public class ModificationRepresenter {

    public static void toJSON(OutputWriter outputWriter, Modification model) {
        outputWriter
                .addIfNotNull("id", model.getId())
                .addIfNotNull("user_name", model.getUserName())
                .addIfNotNull("email_address", model.getEmailAddress())
                .addIfNotNull("revision", model.getRevision())
                .addIfNotNull("modified_time", modifiedTime(model))
                .addIfNotNull("comment", model.getComment());
    }

    public static Consumer<OutputWriter> toJSON(Modification modification) {
        return outputWriter -> toJSON(outputWriter, modification);
    }

    private static Long modifiedTime(Modification model) {
        return model.getModifiedTime() == null ? null : model.getModifiedTime().getTime();
    }
}
