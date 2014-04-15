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

package com.thoughtworks.go.remote.work;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.HttpService;

public class HttpServiceStub extends HttpService {
    private Map<String, File> uploadedFiles = new HashMap<String, File>();
    private List<String> uploadedFileUrls = new ArrayList<String>();

    private int returnCode;

    public HttpServiceStub() {
        this(HttpServletResponse.SC_OK);
    }

    public HttpServiceStub(int returnCode) {
        this.returnCode = returnCode;
    }

    @Override
    public int upload(String url, long size, File artifactFile, java.util.Properties artifactChecksums) throws IOException {
        uploadedFiles.put(url, artifactFile);
        uploadedFileUrls.add(url);

        return returnCode;
    }

    public Map<String, File> getUploadedFiles() {
        return uploadedFiles;
    }
}

