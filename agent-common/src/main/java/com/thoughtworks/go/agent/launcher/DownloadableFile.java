/*
 * Copyright 2023 Thoughtworks, Inc.
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
package com.thoughtworks.go.agent.launcher;

import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.agent.common.util.Downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public enum DownloadableFile {
    AGENT("admin/agent", Downloader.AGENT_BINARY),
    TFS_IMPL("admin/tfs-impl.jar", Downloader.TFS_IMPL),
    LAUNCHER("admin/agent-launcher.jar", Downloader.AGENT_LAUNCHER),
    AGENT_PLUGINS("admin/agent-plugins.zip", Downloader.AGENT_PLUGINS);

    private final String subPath;
    private final String localFileName;

    DownloadableFile(String subPath, String localFileName) {
        this.subPath = subPath;
        this.localFileName = localFileName;
    }

    public String url(ServerUrlGenerator urlGenerator) {
        return urlGenerator.serverUrlFor(subPath);
    }

    public String validatedUrl(ServerUrlGenerator urlGenerator) {
        String url = url(urlGenerator);
        try {
            new URL(url);
        } catch (MalformedURLException mue) {
            throw new RuntimeException(
                    "URL you provided to access Go Server: " + url(urlGenerator) + " is not valid");
        }
        return url;
    }

    @Override
    public String toString() {
        return subPath;
    }

    static boolean matchChecksum(File localFile, String expectedSignature) {
        try (FileInputStream input = new FileInputStream(localFile)) {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            try (DigestInputStream digest = new DigestInputStream(input, digester)) {
                digest.transferTo(OutputStream.nullOutputStream());
            }
            return expectedSignature.equalsIgnoreCase(encodeHexString(digester.digest()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeHexString(byte[] bytes) {
        return String.format("%0" + (bytes.length << 1) + "x", new BigInteger(1, bytes));
    }

    public boolean isChecksumEquals(String expectedSignature) {
        return matchChecksum(getLocalFile(), expectedSignature);
    }

    public boolean doesNotExist() {
        return !new File(localFileName).exists();
    }

    public File getLocalFile() {
        return new File(localFileName);
    }
}
