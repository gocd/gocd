/*
 * Copyright Thoughtworks, Inc.
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

package org.yaml.snakeyaml.constructor;

import org.yaml.snakeyaml.LoaderOptions;

/**
 * Dummy class to workaround https://github.com/liquibase/liquibase/issues/7318 on JDK 25, by providing an
 * implementation that does not depend on snakeyaml.
 */
@SuppressWarnings("unused")
public class SafeConstructor extends BaseConstructor {

    public SafeConstructor(LoaderOptions loaderOptions) {
    }
}
