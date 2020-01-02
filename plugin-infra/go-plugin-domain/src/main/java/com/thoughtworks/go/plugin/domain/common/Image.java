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
package com.thoughtworks.go.plugin.domain.common;

import java.util.Base64;

public class Image {
    private final String contentType;
    private final String data;
    private final byte[] dataAsBytes;

    private final String hash;

    public Image(String contentType, String data, String hash) {
        this.contentType = contentType;
        this.data = data;
        this.hash = hash;
        this.dataAsBytes = Base64.getDecoder().decode(data);
    }

    public String getContentType() {
        return contentType;
    }

    public String getData() {
        return data;
    }

    public String getHash() {
        return hash;
    }

    public byte[] getDataAsBytes() {
        return dataAsBytes;
    }

    public String toDataURI() {
        return "data:" + contentType + ";base64," + data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Image image = (Image) o;

        if (contentType != null ? !contentType.equals(image.contentType) : image.contentType != null) return false;
        if (data != null ? !data.equals(image.data) : image.data != null) return false;
        return hash != null ? hash.equals(image.hash) : image.hash == null;
    }

    @Override
    public int hashCode() {
        int result = contentType != null ? contentType.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (hash != null ? hash.hashCode() : 0);
        return result;
    }
}
