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
package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.Date;

public class NullMaterialRevision extends MaterialRevision {
    public NullMaterialRevision() {
        super(null, new ArrayList<>());
    }

    @Override
    public String buildCausedBy() {
        return "Unknown";
    }

    // we use TimeConverter to do null-safe conversion.  not sure if that is the right way
    @Override
    public Date getDateOfLatestModification() {
        return null;
    }
}
