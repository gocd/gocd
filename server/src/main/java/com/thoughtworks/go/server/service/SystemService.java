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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.dao.DbMetadataDao;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class SystemService {
    private DbMetadataDao dbMetadataDao;

    @Autowired
    public SystemService(DbMetadataDao dbMetadataDao) {
        this.dbMetadataDao = dbMetadataDao;
    }

    public String getProperty(String prop) {
        return SystemEnvironment.getProperty(prop);
    }

    public boolean isAbsolutePath(String artifactsDir) {
        return new File(StringUtils.defaultString(artifactsDir)).isAbsolute();
    }

    public void streamToFile(InputStream stream, File dest) throws IOException {
        try (FileOutputStream out = FileUtils.openOutputStream(dest, true)) {
            IOUtils.copyLarge(stream, out);
        }
    }

    public String getJvmVersion() {
        return SystemEnvironment.getProperty("java.version");
    }

    public String getOsInfo() {
        return SystemEnvironment.getProperty("os.name") + " " + SystemEnvironment.getProperty("os.version");
    }

    public int getSchemaVersion() {
        return dbMetadataDao.getSchemaVersion();
    }

    public void populateServerDetailsModel(Map<String, Object> model) {
        model.put("jvm_version", getJvmVersion());
        model.put("os_info", getOsInfo());
        model.put("schema_version", getSchemaVersion());
    }
}
