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
package com.thoughtworks.go.plugin.infra;


public class GoPluginFrameworkException extends RuntimeException {
    public GoPluginFrameworkException() {
    }

    public GoPluginFrameworkException(String message) {
        super(message);
    }

    public GoPluginFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public GoPluginFrameworkException(Throwable cause) {
        super(cause);
    }
}
