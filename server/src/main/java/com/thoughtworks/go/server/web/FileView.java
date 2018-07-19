/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.web;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.View;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class FileView implements View, ServletContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileView.class);

    private ServletContext servletContext;

    private ServletContext getServletContext() {
        return this.servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public String getContentType() {
        return "application/octet-stream";
    }

    private void handleFile(File file, HttpServletResponse response) throws Exception {
        String filename = file.getName();
        setContentType(response, filename);
        setContentLength(file, response);
        setOutput(file, response);
    }

    private void setOutput(File file, HttpServletResponse response) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        try (InputStream in = new FileInputStream(file)) {
            IOUtils.copy(new FileInputStream(file), out);
        }
        out.flush();
    }

    void setContentLength(File file, HttpServletResponse response) {
        response.addHeader("Content-Length", Long.toString(file.length()));
    }

    private void setContentType(HttpServletResponse response, String filename) {
        response.setContentType(getMimeType(filename));
    }

    private String getMimeType(String filename) {
        String mimeType = this.getServletContext().getMimeType(filename);
        if (StringUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    public void render(Map map, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        File file = (File) map.get("targetFile");
        handleFileWithLogging(httpServletResponse, file);
    }

    private void handleFileWithLogging(HttpServletResponse httpServletResponse, File file) throws Exception {
        LOGGER.info("[Artifact Download] About to download: {}", file.getAbsolutePath());
        long before = System.currentTimeMillis();

        handleFile(file, httpServletResponse);

        long timeTaken = System.currentTimeMillis() - before;
        LOGGER.info("[Artifact Download] Finished downloading: {}. The time taken is: {}ms", file.getAbsolutePath(), timeTaken);
    }

}
