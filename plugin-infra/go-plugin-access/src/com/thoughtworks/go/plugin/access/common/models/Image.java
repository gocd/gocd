/*
 * Copyright 2016 ThoughtWorks, Inc.
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

public class Image {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SerializedName("content_type")
    private final String contentType;

    @Expose
    @SerializedName("data")
    private final String data;

    private final String hash;
    private final byte[] dataAsBytes;
    private final String dataURI;

    public Image(String contentType, String data) {
        this.contentType = contentType;
        this.data = data;

        // lazy eval everything to not take a perf hit on every request
        this.dataURI = "data:" + this.contentType + ";base64," + this.data;
        this.hash = DigestUtils.sha256Hex(this.dataURI);
        this.dataAsBytes = Base64.getDecoder().decode(this.data);
    }

    public String getContentType() {
        return contentType;
    }

    public String getData() {
        return data;
    }

    public String toDataURI() {
        return dataURI;
    }

    public String getHash() {
        return hash;
    }

    public byte[] getDataAsBytes() {
        return dataAsBytes;
    }

    public static Image fromJSON(String responseData) {
        return GSON.fromJson(responseData, Image.class);
    }
}
