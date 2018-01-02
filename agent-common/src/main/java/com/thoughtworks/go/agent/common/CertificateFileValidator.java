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

package com.thoughtworks.go.agent.common;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import com.thoughtworks.go.agent.common.ssl.CertificateFileParser;

import java.io.File;

public class CertificateFileValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value == null || value.trim().isEmpty()) {
            throw new ParameterException(name + " must be specified");
        }

        if (!new File(value).canRead()) {
            throw new ParameterException(name + " must be a file that is readable");
        }

        try {
            if (new CertificateFileParser().certificates(new File(value)).isEmpty()) {
                throw badCertfile(name);
            }
        } catch (Exception e) {
            throw badCertfile(name);
        }
    }

    private ParameterException badCertfile(String name) {
        return new ParameterException(name + " must contain one or more X.509 certificates in DER encoded " +
                "and may be supplied in binary or Base64 encoding");
    }

}
