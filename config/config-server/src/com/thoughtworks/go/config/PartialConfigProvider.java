/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.config;


import com.thoughtworks.go.config.remote.PartialConfig;

import java.io.File;

/**
 * Can obtain configuration objects from a source code tree.
 * Possible extension point for custom pipeline configuration format.
 * Expects a checked-out source code tree.
 * It does not understand versioning.
 * Each implementation defines its own pattern
 * to identify configuration files in repository structure.
 */
public interface PartialConfigProvider {

    // TODO consider: could have Parse() whose result is
    // stored by Go in memory so that single checkout is parsed only once.

    PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context);

    String displayName();

    // any further elements that could be obtained from config repo
}
