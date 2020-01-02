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
package com.thoughtworks.go.config;

import java.util.ArrayList;

/*
CachedPluggableArtifactConfigs is used only for caching purposes.
This class is marked to set null instead of clone at GoConfigCloner,
which will cause Cloner to not clone CachedPluggableArtifactConfigs.
*/

public class CachedPluggableArtifactConfigs extends ArrayList<PluggableArtifactConfig> {
}
