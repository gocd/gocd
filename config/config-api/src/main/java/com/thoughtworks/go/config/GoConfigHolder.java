/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.util.CachedDigestUtils;

import java.util.List;
import java.util.Objects;

public class GoConfigHolder {
    public final CruiseConfig config;
    private final CruiseConfig configForEdit;
    private CruiseConfig mergedConfigForEdit;
    private Checksum checksum;

    public GoConfigHolder(CruiseConfig config, CruiseConfig configForEdit) {
        this(config, configForEdit, null, null);
    }

    public GoConfigHolder(CruiseConfig config, CruiseConfig configForEdit, CruiseConfig mergedConfigForEdit, List<PartialConfig> partials) {
        this.config = config;
        this.configForEdit = configForEdit;
        setMergedConfigForEdit(mergedConfigForEdit, partials);
    }

    private String md5(List<PartialConfig> partials) {
        if (partials == null || partials.isEmpty()) {
            return null;
        }
        return CachedDigestUtils.md5Hex(partials);
    }

    public Checksum getChecksum() {
        return checksum;
    }

    public CruiseConfig getMergedConfigForEdit() {
        return mergedConfigForEdit;
    }

    public void setMergedConfigForEdit(CruiseConfig mergedConfigForEdit, List<PartialConfig> partials) {
        this.mergedConfigForEdit = mergedConfigForEdit;
        this.checksum = new Checksum(configForEdit.getMd5(), md5(partials));
    }

    public CruiseConfig getConfigForEdit() {
        return configForEdit;
    }

    public static class Checksum {
        public String md5SumOfConfigForEdit;
        public String md5SumOfPartials;

        public Checksum(String md5SumOfConfigForEdit, String md5SumOfPartials) {
            this.md5SumOfConfigForEdit = md5SumOfConfigForEdit;
            this.md5SumOfPartials = md5SumOfPartials;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Checksum checksum = (Checksum) o;
            return Objects.equals(md5SumOfConfigForEdit, checksum.md5SumOfConfigForEdit) &&
                    Objects.equals(md5SumOfPartials, checksum.md5SumOfPartials);
        }

        @Override
        public int hashCode() {
            return Objects.hash(md5SumOfConfigForEdit, md5SumOfPartials);
        }
    }
}