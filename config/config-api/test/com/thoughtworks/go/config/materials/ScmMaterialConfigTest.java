/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config.materials;

import java.util.Collections;
import java.util.HashMap;

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import org.apache.commons.collections.map.SingletonMap;
import org.junit.Test;

import static com.thoughtworks.go.config.materials.ScmMaterialConfig.AUTO_UPDATE;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ScmMaterialConfigTest {
    DummyMaterialConfig material = new DummyMaterialConfig();

    @Test
    public void shouldSetFilterToNullWhenBlank() {
        material.setFilter(new Filter(new IgnoredFiles("*.*")));
        material.setConfigAttributes(new SingletonMap(ScmMaterialConfig.FILTER, ""));
        assertThat(material.filter(), is(new Filter()));
        assertThat(material.getFilterAsString(), is(""));
    }

    @Test
    public void shouldReturnFilterForDisplay() {
        material.setFilter(new Filter(new IgnoredFiles("/foo/**.*"), new IgnoredFiles("/another/**.*"), new IgnoredFiles("bar")));
        assertThat(material.getFilterAsString(), is("/foo/**.*,/another/**.*,bar"));
    }

    @Test
    public void shouldNotValidateEmptyDestinationFolder() {
        material.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, ""));
        material.validate(new ConfigSaveValidationContext(null));
        assertThat(material.errors.isEmpty(), is(true));
    }

    @Test
    public void shouldSetFolderToNullWhenBlank() {
        material.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "foo"));
        assertThat(material.getFolder(), is(not(nullValue())));

        material.setConfigAttributes(new SingletonMap(ScmMaterialConfig.FOLDER, ""));
        assertThat(material.getFolder(), is(nullValue()));
    }

    @Test
    public void shouldUpdateAutoUpdateFieldFromConfigAttributes() {
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, "false"));
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, null));
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, "true"));
        assertThat(material.isAutoUpdate(), is(true));
        material.setConfigAttributes(new HashMap());
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, null));
        assertThat(material.isAutoUpdate(), is(false));
        material.setConfigAttributes(new SingletonMap(AUTO_UPDATE, "random-stuff"));
        assertThat(material.isAutoUpdate(), is(false));
    }
}
