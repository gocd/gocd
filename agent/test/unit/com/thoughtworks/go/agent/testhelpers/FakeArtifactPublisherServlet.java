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

package com.thoughtworks.go.agent.testhelpers;

import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FakeArtifactPublisherServlet extends HttpServlet {

    private static HashSet<String> receivedFiles = new HashSet<>();
    private static StringBuilder consoleOutput = new StringBuilder();

    public static Set<String> receivedFiles() throws InterruptedException {
        return receivedFiles;
    }

    public static String consoleOutput() throws InterruptedException {
        return consoleOutput.toString();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        MultipartHttpServletRequest httpServletRequest = multipartResolver.resolveMultipart(request);
        Map<String, MultipartFile> map = httpServletRequest.getFileMap();
        MultipartFile multipartFile = map.values().iterator().next();
        receivedFiles.add(multipartFile.getOriginalFilename());
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String str = IOUtils.toString(request.getInputStream());
        consoleOutput.append(str);
    }
}
