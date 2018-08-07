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

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.server.domain.Username;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.output.NullOutputStream;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Set;

public interface DashboardGroup {
    String name();

    Set<String> pipelines();

    boolean canAdminister(Username username);

    String etag();

    default String digest(String permissionsSegment, Collection<GoDashboardPipeline> pipelines) {
        try {
            MessageDigest digest = DigestUtils.getSha256Digest();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new DigestOutputStream(new NullOutputStream(), digest));
            outputStreamWriter.write(getClass().getSimpleName());
            outputStreamWriter.write("$");
            outputStreamWriter.write(name());
            outputStreamWriter.write("/");
            outputStreamWriter.write(permissionsSegment);
            outputStreamWriter.write("[");

            for (GoDashboardPipeline pipeline : pipelines) {
                outputStreamWriter.write(pipeline.cacheSegment());
                outputStreamWriter.write(",");
            }

            outputStreamWriter.write("]");
            outputStreamWriter.flush();

            return Hex.encodeHexString(digest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
