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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.zip.Deflater;

@Component
public class FileView implements View, ServletContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileView.class);

    private ServletContext servletContext;
    public static final String NEED_TO_ZIP = "need_to_zip";

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

    private void handleFile(File file, boolean needToZip, HttpServletResponse response) throws Exception {
        String filename = file.getName();
        seContentType(needToZip, response, filename);
        setHeaders(response, filename);
        setContentLength(needToZip, file, response);
        setOutput(needToZip, file, response);
    }

    private void setOutput(boolean needToZip, File file, HttpServletResponse response) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        if (needToZip) {
            new ZipUtil().zip(file, out, Deflater.NO_COMPRESSION);
        } else {
            try (FileInputStream input = new FileInputStream(file)) {
                IOUtils.copy(input, out, 32 * 1024);
            }
        }
        out.flush();
    }

    void setContentLength(boolean needToZip, File file, HttpServletResponse response) {
        if (!needToZip) {
            response.addHeader("Content-Length", Long.toString(file.length()));
        }
    }

    private void setHeaders(HttpServletResponse response, String filename) {
        if (filename.equals("console.log")) {
            response.setHeader("Content-Disposition", "Inline; filename=fname.ext");
        }
    }

    private void seContentType(boolean needToZip, HttpServletResponse response, String filename) {
        response.setContentType(getMimeType(filename, needToZip));
    }

    private String getMimeType(String filename, boolean needToZip) {
        if (needToZip) {
            return "application/zip";
        }

        String mimeType = this.getServletContext().getMimeType(filename);
        if (StringUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    @Override
    public void render(Map map, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        File file = (File) map.get("targetFile");
        boolean needToZip = map.containsKey(NEED_TO_ZIP);
        handleFileWithLogging(httpServletResponse, file, needToZip);
    }

    private void handleFileWithLogging(HttpServletResponse httpServletResponse, File file, boolean needToZip) throws Exception {
        LOGGER.info("[Artifact Download] About to download: {}. ShouldZip? = {}", file.getAbsolutePath(), needToZip);
        long before = System.currentTimeMillis();

        handleFile(file, needToZip, httpServletResponse);

        long timeTaken = System.currentTimeMillis() - before;
        LOGGER.info("[Artifact Download] Finished downloading: {}. ShouldZip? = {}. The time taken is: {}ms", file.getAbsolutePath(), needToZip, timeTaken);
    }

}
