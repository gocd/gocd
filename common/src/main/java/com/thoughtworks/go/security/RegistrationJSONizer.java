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

package com.thoughtworks.go.security;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class RegistrationJSONizer {
    private static final Gson GSON = new Gson();

    public static Registration fromJson(String json) {
        Map map = GSON.fromJson(json, Map.class);

        if (map.isEmpty()) {
            return Registration.createNullPrivateKeyEntry();
        }

        List<Certificate> chain = new ArrayList<>();
        try {
            PemReader reader = new PemReader(new StringReader((String) map.get("agentPrivateKey")));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(reader.readPemObject().getContent());
            PrivateKey privateKey = kf.generatePrivate(spec);
            String agentCertificate = (String) map.get("agentCertificate");
            PemReader certReader = new PemReader(new StringReader(agentCertificate));
            while (true) {
                PemObject obj = certReader.readPemObject();
                if (obj == null) {
                    break;
                }
                chain.add(CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(obj.getContent())));
            }
            return new Registration(privateKey, chain.toArray(new Certificate[chain.size()]));
        } catch (IOException | NoSuchAlgorithmException | CertificateException | InvalidKeySpecException e) {
            throw bomb(e);
        }
    }


    public static String toJson(Registration registration) {
        Map<String, Object> ret = new HashMap<>();

        if (registration.isValid()) {
            ret.put("agentPrivateKey", serialize("RSA PRIVATE KEY", registration.getPrivateKey().getEncoded()));
            StringBuilder builder = new StringBuilder();
            for (Certificate c : registration.getChain()) {
                try {
                    builder.append(serialize("CERTIFICATE", c.getEncoded()));
                } catch (CertificateEncodingException e) {
                    throw bomb(e);
                }
            }
            ret.put("agentCertificate", builder.toString());
        }

        return GSON.toJson(ret);
    }

    private static String serialize(String type, byte[] data) {
        PemObject obj = new PemObject(type, data);
        StringWriter out = new StringWriter();
        PemWriter writer = new PemWriter(out);
        try {
            writer.writeObject(obj);
        } catch (IOException e) {
            throw bomb(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return out.toString();
    }
}
