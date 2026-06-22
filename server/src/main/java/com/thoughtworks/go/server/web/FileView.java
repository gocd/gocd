/*
 * Copyright Thoughtworks, Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.View;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
public class FileView implements View, ServletContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileView.class);

    private ServletContext servletContext;

    private ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    private void handleFile(File file, HttpServletResponse response) throws IOException {
        String filename = file.getName();
        seContentType(response, filename);
        setHeaders(response, filename);
        setContentLength(file, response);
        setOutput(file, response);
    }

    private void setOutput(File file, HttpServletResponse response) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        try (FileInputStream input = new FileInputStream(file)) {
            input.transferTo(out);
        }
        out.flush();
    }

    void setContentLength(File file, HttpServletResponse response) {
        response.addHeader("Content-Length", Long.toString(file.length()));
    }

    private void setHeaders(HttpServletResponse response, String filename) {
        if (filename.equals("console.log")) {
            response.setHeader("Content-Disposition", "Inline; filename=fname.ext");
        }
    }

    private void seContentType(HttpServletResponse response, String filename) {
        response.setContentType(getMimeType(filename));
    }

    private String getMimeType(String filename) {
        String mimeType = this.getServletContext().getMimeType(filename);
        if (isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        File file = (File) model.get("targetFile");
        handleFileWithLogging(httpServletResponse, file);
    }

    private void handleFileWithLogging(HttpServletResponse httpServletResponse, File file) throws IOException {
        LOGGER.info("[Artifact Download] About to download: {}. ShouldZip? = {}", file.getAbsolutePath(), false);
        long before = System.currentTimeMillis();

        handleFile(file, httpServletResponse);

        long timeTaken = System.currentTimeMillis() - before;
        LOGGER.info("[Artifact Download] Finished downloading: {}. ShouldZip? = {}. The time taken is: {} ms", file.getAbsolutePath(), false, timeTaken);
    }

}
