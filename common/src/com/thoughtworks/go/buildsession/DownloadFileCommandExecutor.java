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
import com.thoughtworks.go.domain.ChecksumFileHandler;
import com.thoughtworks.go.domain.FileHandler;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.go.util.URLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class DownloadFileCommandExecutor implements BuildCommandExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadFileCommandExecutor.class);

    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        URLService urlService = new URLService();
        String url = urlService.prefixPartialUrl(command.getStringArg("url"));
        String dest = command.getStringArg("dest");
        String src = command.getStringArg("src");
        File artifact = buildSession.resolveRelativeDir(command.getWorkingDirectory(), dest);
        FileHandler handler = new FileHandler(artifact, src);
        String checksumUrl = null;
        ChecksumFileHandler checksumFileHandler = null;

        if (command.hasArg("checksumUrl")) {
            checksumUrl = new URLService().prefixPartialUrl(command.getStringArg("checksumUrl"));
            File checksumFile;
            if (command.hasArg("checksumFile")) {
                checksumFile = buildSession.resolveRelativeDir(command.getWorkingDirectory(), command.getStringArg("checksumFile"));
            } else {
                checksumFile = TempFiles.createUniqueFile("checksum");
            }
            checksumFileHandler = new ChecksumFileHandler(checksumFile);
        }


        boolean fileExist = artifact.exists();
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Requesting the file [" + artifact.getAbsolutePath() + "], exist? [" + fileExist + "]");
        }

        if (fileExist && artifact.isFile()) {
            try {
                url += "?sha1=" + java.net.URLEncoder.encode(StringUtil.sha1Digest(artifact), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LOG.error("Download error", e);
                return false;
            }
        }

        buildSession.download(handler, url, checksumFileHandler, checksumUrl);
        return true;

    }
}
