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

package com.thoughtworks.go.apiv1.compare.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.materials.Modification;

public class ModificationRepresenter {
    public static void toJSON(OutputWriter outputWriter, Modification modification) {
        outputWriter
                .add("revision", modification.getRevision())
                .addIfNotNull("modified_time", modification.getModifiedTime())
                .add("user_name", modification.getUserName())
                .add("comment", modification.getComment())
                .add("email_address", modification.getEmailAddress());
    }
}
