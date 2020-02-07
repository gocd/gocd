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

package com.thoughtworks.go.apiv1.webhook.request.mixins.bitbucketserver;

import com.thoughtworks.go.apiv1.webhook.request.mixins.HasAuth;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.h2.util.Utils;

import static org.apache.commons.lang3.StringUtils.isBlank;

public interface BitBucketServerAuth extends HasAuth {
    default void validateAuth(String webhookSecret) {
        if (exemptFromAuth()) {
            return;
        }

        String signature = request().headers("X-Hub-Signature");

        if (isBlank(signature)) {
            throw die("No HMAC signature specified via 'X-Hub-Signature' header!");
        }

        String expectedSignature = "sha256=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_256, webhookSecret)
                .hmacHex(request().body());

        if (!Utils.compareSecure(expectedSignature.getBytes(), signature.getBytes())) {
            throw die("HMAC signature specified via 'X-Hub-Signature' did not match!");
        }

        if (!"git".equals(scmType())) {
            throw die("Only 'git' repositories are currently supported!");
        }
    }

    boolean exemptFromAuth();

    String scmType();
}
