/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.agent.HttpService;
import com.thoughtworks.go.domain.FetchHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HttpServiceStub extends HttpService {
    private final Map<String, File> uploadedFiles = new HashMap<>();

    private final int returnCode;

    public HttpServiceStub() {
        this(HttpServletResponse.SC_OK);
    }

    public HttpServiceStub(int returnCode) {
        super(null, null);
        this.returnCode = returnCode;
    }

    @Override
    public int upload(String url, long size, File artifactFile, Properties artifactChecksums) throws IOException {
        uploadedFiles.put(url, artifactFile);
        return returnCode;
    }

    public Map<String, File> getUploadedFiles() {
        return uploadedFiles;
    }

    @Override
    public int download(String url, FetchHandler handler) {
        throw new UnsupportedOperationException("download not implemented");
    }
}

