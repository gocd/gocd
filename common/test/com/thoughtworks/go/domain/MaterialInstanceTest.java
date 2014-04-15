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

package com.thoughtworks.go.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.helper.MaterialsMother;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class MaterialInstanceTest {
    @Test
    public void shouldGenerateUniqueFingerprintOnCreation() throws Exception {
        MaterialInstance one = new HgMaterial("url", null).createMaterialInstance();
        MaterialInstance two = new HgMaterial("otherurl", null).createMaterialInstance();
        assertThat(one.getFingerprint(), not(nullValue()));
        assertThat(one.getFingerprint(), not(is(two.getFingerprint())));
    }

    @Test
    public void shouldSerializeAndUnserializeAllAttributes() throws IOException, ClassNotFoundException {
        HgMaterial m = MaterialsMother.hgMaterial("url");
        MaterialInstance materialInstance = m.createMaterialInstance();
        materialInstance.setId(10);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(materialInstance);
        ObjectInputStream inputStream1 = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        MaterialInstance unserializedMaterial = (MaterialInstance) inputStream1.readObject();
        assertThat(unserializedMaterial, Matchers.is(materialInstance));
        assertThat(unserializedMaterial.getId(), is(10L));
        assertThat(unserializedMaterial, is(materialInstance));
    }
}