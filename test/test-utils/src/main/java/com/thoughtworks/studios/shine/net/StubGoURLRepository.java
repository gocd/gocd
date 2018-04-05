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

package com.thoughtworks.studios.shine.net;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StubGoURLRepository implements URLRepository {
  private String baseDir;
  private final String artifactRoot;

  public StubGoURLRepository(String webserverRoot, String artifactRoot) {
    this.artifactRoot = artifactRoot;
    this.baseDir = webserverRoot;
  }

  public void registerStubContent(String url, String content) {
    String baseFile = url.replaceFirst("http://localhost:3000/go", "");

    File file = new File(baseDir, baseFile);
    file.getParentFile().mkdirs();

    try {
      FileUtils.writeStringToFile(file, content, UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Could not write to file", e);
    }
  }

  public void registerException(String url, IOException toThrow) {
    throw new UnsupportedOperationException("Don't call me.");
  }

  public void registerArtifact(String path, String content) {
    File file = new File(artifactRoot, path);
    file.getParentFile().mkdirs();

    try {
      FileUtils.writeStringToFile(file, content, UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Could not write to file", e);
    }

  }

  public String getBaseDir() {
    return this.baseDir;
  }
}
