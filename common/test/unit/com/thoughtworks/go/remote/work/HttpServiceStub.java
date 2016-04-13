/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.domain.FetchHandler;
import com.thoughtworks.go.util.HttpService;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class HttpServiceStub extends HttpService {
    private Map<String, File> uploadedFiles = new HashMap<String, File>();
    private Map<String, byte[]> downloadFiles = new HashMap<>();
    private List<String> uploadedFileUrls = new ArrayList<String>();

    private int returnCode;

    public HttpServiceStub() {
        this(HttpServletResponse.SC_OK);
    }

    public HttpServiceStub(int returnCode) {
        this.returnCode = returnCode;
    }

    @Override
    public int upload(String url, long size, File artifactFile, Properties artifactChecksums) throws IOException {
        uploadedFiles.put(url, artifactFile);
        uploadedFileUrls.add(url);

        return returnCode;
    }

    public Map<String, File> getUploadedFiles() {
        return uploadedFiles;
    }

    @Override
    public int download(String url, FetchHandler handler) throws IOException {
        byte[] body = downloadFiles.get(url);
        if(body == null) {
            return HttpServletResponse.SC_NOT_FOUND;
        }
        handler.handle(new ByteArrayInputStream(body));
        return returnCode;
    }

    public void setupDownload(String url, String body) {
        downloadFiles.put(url, body.getBytes());
    }

    public void setupDownload(String url, File file) throws IOException {
        downloadFiles.put(url, IOUtils.toByteArray(new FileInputStream(file)));
    }

}

