/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.common.models;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Base64;

@Deprecated
public class Image {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SerializedName("content_type")
    private final String contentType;

    @Expose
    @SerializedName("data")
    private final String data;

    private String hash;
    private byte[] dataAsBytes;
    private String dataURI;

    public Image(String contentType, String data) {
        this.contentType = contentType;
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public String getData() {
        return data;
    }

    @Deprecated // used only in v2 plugin info, which is going away
    public String toDataURI() {
        if (dataURI == null) {
            dataURI = "data:" + contentType + ";base64," + data;
        }
        return dataURI;
    }

    public String getHash() {
        if (hash == null) {
            hash = DigestUtils.sha256Hex(toDataURI());
        }
        return hash;
    }

    public byte[] getDataAsBytes() {
        if (dataAsBytes == null) {
            dataAsBytes = Base64.getDecoder().decode(data);
        }
        return dataAsBytes;
    }

    public static Image fromJSON(String responseData) {
        return GSON.fromJson(responseData, Image.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Image image = (Image) o;

        if (contentType != null ? !contentType.equals(image.contentType) : image.contentType != null) return false;
        return data != null ? data.equals(image.data) : image.data == null;

    }

    @Override
    public int hashCode() {
        int result = contentType != null ? contentType.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}
