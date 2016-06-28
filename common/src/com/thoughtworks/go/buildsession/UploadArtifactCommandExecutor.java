/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.WildcardScanner;
import org.apache.commons.lang.StringUtils;

import java.io.File;

import static com.thoughtworks.go.util.FileUtil.normalizePath;
import static com.thoughtworks.go.util.FileUtil.subtractPath;
import static com.thoughtworks.go.util.SelectorUtils.rtrimStandardrizedWildcardTokens;
import static org.apache.commons.lang.StringUtils.removeStart;

public class UploadArtifactCommandExecutor implements BuildCommandExecutor {
    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        final String src = command.getStringArg("src");
        final String dest = command.getStringArg("dest");
        final Boolean ignoreUnmatchError = command.getBooleanArg("ignoreUnmatchError");
        final File rootPath = buildSession.resolveRelativeDir(command.getWorkingDirectory());

        WildcardScanner scanner = new WildcardScanner(rootPath, src);
        File[] files = scanner.getFiles();

        if (files.length == 0) {
            String message = "The rule [" + src + "] cannot match any resource under [" + rootPath + "]";
            buildSession.printlnWithPrefix(message);
            return ignoreUnmatchError;
        }

        for (File file : files) {
            buildSession.upload(file, destURL(rootPath, file, src, dest));
        }
        return true;
    }

    protected String destURL(File rootPath, File file, String src, String dest) {
        String trimmedPattern = rtrimStandardrizedWildcardTokens(src);
        if (StringUtils.equals(normalizePath(trimmedPattern), normalizePath(src))) {
            return dest;
        }
        String trimmedPath = removeStart(subtractPath(rootPath, file), normalizePath(trimmedPattern));
        if (!StringUtils.startsWith(trimmedPath, "/") && StringUtils.isNotEmpty(trimmedPath)) {
            trimmedPath = "/" + trimmedPath;
        }
        return dest + trimmedPath;
    }

}
