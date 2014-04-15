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

package com.thoughtworks.go.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class FileDigester {
    private final InputStream input;
    private final OutputStream output;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private MessageDigest md;

    public FileDigester(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    // Copied from IOUtils with extra stuff for digesting
    public long copy() throws IOException {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw ExceptionUtils.bomb(e);
        }
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            md.update(buffer, 0, n);
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public String md5() {
        if (md == null) {
            throw new IllegalStateException("You must call copy() to copy the file before trying to obtain the digest");
        }
        return StringUtil.base64Encode(md.digest());
    }

    public static String md5DigestOfFile(File file) throws IOException {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            return md5DigestOfStream(stream);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static String md5DigestOfStream(InputStream stream) throws IOException {
        return copyAndDigest(stream, new NullOutputStream());
    }

    public static String copyAndDigest(InputStream inputStream, OutputStream outputStream) throws IOException {
        FileDigester digester = new FileDigester(inputStream, outputStream);
        digester.copy();
        return digester.md5();
    }
}
