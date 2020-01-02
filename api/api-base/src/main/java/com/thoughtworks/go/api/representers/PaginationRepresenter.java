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
package com.thoughtworks.go.api.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.server.util.Pagination;

import java.util.function.Consumer;

public class PaginationRepresenter {
    public static void toJSON(OutputWriter jsonWriter, Pagination pagination) {
        jsonWriter.add("page_size", pagination.getPageSize())
                .add("offset", pagination.getOffset())
                .add("total", pagination.getTotal());
    }

    public static Consumer<OutputWriter> toJSON(Pagination pagination) {
        return outputWriter -> PaginationRepresenter.toJSON(outputWriter, pagination);
    }
}
