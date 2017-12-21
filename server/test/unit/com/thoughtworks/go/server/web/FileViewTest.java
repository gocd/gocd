/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import org.apache.commons.io.FileUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.when;

public class FileViewTest extends MockObjectTestCase {
    private MockHttpServletResponse mockResponse;

    private MockHttpServletRequest mockRequest;

    private FileView view;

    private Mock mockServletContext;
    private File file;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected void setUp() throws Exception {
        temporaryFolder.create();
        super.setUp();
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();
        mockServletContext = mock(ServletContext.class);
        view = new FileView();
        view.setServletContext((ServletContext) mockServletContext.proxy());
        file = temporaryFolder.newFile("file.txt");
        FileUtils.writeStringToFile(file, "hello", UTF_8);
    }

    public void tearDown() throws Exception {
        temporaryFolder.delete();
    }

    public void testShouldNotTruncateTheContentLengthHeaderIfTheLengthIsGreaterThan2G() {
        File fourGBfile = Mockito.mock(File.class);
        long fourGB = 4658798592L;
        when(fourGBfile.length()).thenReturn(fourGB);

        HttpServletResponse responseMock = Mockito.mock(HttpServletResponse.class);
        view.setContentLength(false, fourGBfile, responseMock);

        Mockito.verify(responseMock).addHeader("Content-Length", "4658798592");
        Mockito.verifyNoMoreInteractions(responseMock);
    }

    public void testShouldOutputTxtFileContentAsTextPlain() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("targetFile", file);
        mockServletContext.expects(once()).method("getMimeType")
                .will(returnValue(RESPONSE_CHARSET));
        view.render(model, mockRequest, mockResponse);
        assertEquals(RESPONSE_CHARSET, mockResponse.getContentType());
        assertEquals(5, getContentLength(mockResponse));
    }

    public void testShouldZipFileIfZipIsRequired() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("targetFile", file);
        model.put(FileView.NEED_TO_ZIP, true);

        view.render(model, mockRequest, mockResponse);

        // Unzip from the response and verify that the we can read the file back
        File unzipHere = temporaryFolder.newFolder();
        new ZipUtil().unzip(
                new ZipInputStream(new ByteArrayInputStream(mockResponse.getContentAsByteArray())), unzipHere);
        assertEquals(FileUtils.readFileToString(new File(unzipHere, file.getName()), UTF_8), "hello");
    }

    public void testShouldNotZipIfZipIsNotRequired() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("targetFile", file);

        mockServletContext.expects(once()).method("getMimeType")
                .will(returnValue(RESPONSE_CHARSET));

        view.render(model, mockRequest, mockResponse);

        // Read from the response.  The file should not be zipped
        assertEquals(mockResponse.getContentAsString(), "hello");
    }

    public void testDefaultContentTypeShouldBeTextPlain() throws Exception {
        assertEquals("application/octet-stream", view.getContentType());
    }

    public void testCharacterEncodingSetToUtf8ForConsoleLogfile() throws Exception {
        file = temporaryFolder.newFile("console.log");
        Map<String, Object> model = new HashMap<>();
        model.put("targetFile", file);

        mockServletContext.expects(once()).method("getMimeType").will(returnValue("text/plain;charset=utf-8"));

        view.render(model, mockRequest, mockResponse);
        assertEquals("text/plain;charset=utf-8", mockResponse.getContentType());
        assertEquals("utf-8", mockResponse.getCharacterEncoding());
    }

    private long getContentLength(MockHttpServletResponse mockResponse) {
        return Long.parseLong(mockResponse.getHeader("Content-Length").toString());
    }
}
