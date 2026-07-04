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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.server.controller.actions.TextAction.CONTENT_TYPE;
import static com.thoughtworks.go.util.TempDirUtils.newFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class FileViewTest {
    @TempDir
    Path tempDir;

    private MockHttpServletResponse mockResponse;

    private MockHttpServletRequest mockRequest;

    private FileView view;

    private ServletContext mockServletContext;
    private File file;

    @BeforeEach
    public void setUp() throws Exception {
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();
        mockServletContext = mock(ServletContext.class);
        view = new FileView();
        view.setServletContext(mockServletContext);
        file = newFile(tempDir.resolve("file.txt"));
        Files.writeString(file.toPath(), "hello", UTF_8);
    }

    @Test
    public void testShouldNotTruncateTheContentLengthHeaderIfTheLengthIsGreaterThan2G() {
        File fourGBfile = mock(File.class);
        long fourGB = 4658798592L;
        when(fourGBfile.length()).thenReturn(fourGB);

        HttpServletResponse responseMock = mock(HttpServletResponse.class);
        view.setContentLength(fourGBfile, responseMock);

        verify(responseMock).addHeader("Content-Length", "4658798592");
        verifyNoMoreInteractions(responseMock);
    }

    @Test
    public void testShouldOutputTxtFileContentAsTextPlain() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("targetFile", file);
        when(mockServletContext.getMimeType(any())).thenReturn(CONTENT_TYPE);
        view.render(model, mockRequest, mockResponse);
        assertEquals(CONTENT_TYPE, mockResponse.getContentType());
        assertEquals(5, getContentLength(mockResponse));
    }

    @Test
    public void testShouldNotZipIfZipIsNotRequired() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("targetFile", file);

        when(mockServletContext.getMimeType(any())).thenReturn(CONTENT_TYPE);

        view.render(model, mockRequest, mockResponse);

        // Read from the response.  The file should not be zipped
        assertEquals("hello", mockResponse.getContentAsString());
    }

    @Test
    public void testDefaultContentTypeShouldBeTextPlain() {
        assertEquals("application/octet-stream", view.getContentType());
    }

    @Test
    public void testCharacterEncodingSetToUtf8ForConsoleLogfile() throws Exception {
        file = newFile(tempDir.resolve("console.log"));
        Map<String, Object> model = new HashMap<>();
        model.put("targetFile", file);

        when(mockServletContext.getMimeType(any())).thenReturn("text/plain;charset=utf-8");

        view.render(model, mockRequest, mockResponse);
        assertEquals("text/plain;charset=utf-8", mockResponse.getContentType());
        assertEquals("UTF-8", mockResponse.getCharacterEncoding());
    }

    private long getContentLength(MockHttpServletResponse mockResponse) {
        return Long.parseLong(mockResponse.getHeader("Content-Length"));
    }
}
