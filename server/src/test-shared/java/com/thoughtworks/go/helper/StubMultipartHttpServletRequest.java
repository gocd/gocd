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
package com.thoughtworks.go.helper;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

public class StubMultipartHttpServletRequest extends DefaultMultipartHttpServletRequest {

    private MockHttpServletRequest mockHttpReq;
    private final Map<String, MultipartFile> map = new HashMap<>();

    public StubMultipartHttpServletRequest(MockHttpServletRequest mockHttpReq, MultipartFile... file) {
        super(mockHttpReq);
        for (MultipartFile multipartFile : file) {
            map.put(multipartFile.getName(), multipartFile);
        }
        this.mockHttpReq = mockHttpReq;
    }

    @Override
    public Map getFileMap() {
        return map;
    }

    @Override
    public String getRequestURI() {
        return mockHttpReq.getRequestURI();
    }

    @Override
    public String[] getParameterValues(String s) {
        return mockHttpReq.getParameterValues(s);
    }

    @Override
    public String getParameter(String name) {
        return mockHttpReq.getParameter(name);
    }

    @Override
    public MultipartFile getFile(String name) {
        return map.get(name);
    }

    @Override
    protected void initializeMultipart() {
    }
}
