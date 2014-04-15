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

package com.thoughtworks.go.server.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.Deflater;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.View;

public class FileView implements View, ServletContextAware {

    private static final Logger LOGGER = Logger.getLogger(FileView.class);

    private ServletContext servletContext;
    public static final String NEED_TO_ZIP = "need_to_zip";

    private ServletContext getServletContext() {
        return this.servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

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
            IOUtils.copy(new FileInputStream(file), out);
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
        if("console.log".equals(filename)){
            return "text/plain; charset=utf-8";
        }
        String mimeType = this.getServletContext().getMimeType(filename);
        if (StringUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    public void render(Map map, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        File file = (File) map.get("targetFile");
        boolean needToZip = map.containsKey(NEED_TO_ZIP);
        handleFileWithLogging(httpServletResponse, file, needToZip);
    }

    private void handleFileWithLogging(HttpServletResponse httpServletResponse, File file, boolean needToZip) throws Exception {
        LOGGER.info(String.format("[Artifact Download] About to download: %s. ShouldZip? = %s", file.getAbsolutePath(), needToZip));
        long before = System.currentTimeMillis();

        handleFile(file, needToZip, httpServletResponse);

        long timeTaken = System.currentTimeMillis() - before;
        LOGGER.info(String.format("[Artifact Download] Finished downloading: %s. ShouldZip? = %s. The time taken is: %sms", file.getAbsolutePath(), needToZip, timeTaken));
    }

}
