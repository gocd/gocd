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

package com.thoughtworks.go.server.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.thoughtworks.go.server.dao.DbMetadataDao;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemService {
    private DbMetadataDao dbMetadataDao;
    private SystemEnvironment systemEnvironment;
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemService.class);

    @Autowired
    public SystemService(DbMetadataDao dbMetadataDao, SystemEnvironment systemEnvironment) {
        this.dbMetadataDao = dbMetadataDao;
        this.systemEnvironment = systemEnvironment;
    }

    public String getProperty(String prop) {
        return SystemEnvironment.getProperty(prop);
    }

    public boolean isAbsolutePath(String artifactsDir) {
        return new File(StringUtils.defaultString(artifactsDir)).isAbsolute();
    }

    public void streamToFile(InputStream stream, File dest) throws IOException {
        dest.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(dest, true);
        try {
            IOUtils.copyLarge(stream, out);
        } finally {
            IOUtils.closeQuietly(out);
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
