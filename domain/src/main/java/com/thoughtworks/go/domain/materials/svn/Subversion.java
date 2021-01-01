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
package com.thoughtworks.go.domain.materials.svn;


import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Subversion {

    void updateTo(ConsoleOutputStreamConsumer outputStreamConsumer, File workingFolder,
                  SubversionRevision targetRevision);

    List<Modification> latestModification();

    ValidationBean checkConnection();

    String getUrlForDisplay();

    String workingRepositoryUrl(File workingFolder) throws IOException;

    void cleanupAndRevert(ConsoleOutputStreamConsumer outputStreamConsumer, File workingFolder);

    String getUserName();

    String getPassword();

    boolean isCheckExternals();

    SubversionRevision checkoutTo(ConsoleOutputStreamConsumer outputStreamConsumer, File targetFolder,
                                  SubversionRevision revision);

    void add(ConsoleOutputStreamConsumer outputStreamConsumer, File file);

    void commit(ConsoleOutputStreamConsumer outputStreamConsumer, File workingDir, String message);

    List<Modification> modificationsSince(SubversionRevision subversionRevision);

    List<SvnExternal> getAllExternalURLs();

    UrlArgument getUrl();
}
