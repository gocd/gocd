/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.security;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import sun.security.x509.X509CertImpl;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class Registration implements Serializable {

    public static Registration fromJson(String json) {
        Map map = new Gson().fromJson(json, Map.class);
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
                chain.add(new X509CertImpl(obj.getContent()));
            }
            return new Registration(privateKey, chain.toArray(new Certificate[chain.size()]));
        } catch (IOException | NoSuchAlgorithmException | CertificateException | InvalidKeySpecException e) {
            throw bomb(e);
        }
    }

    private final PrivateKey privateKey;
    private final Certificate[] chain;

    public static Registration createNullPrivateKeyEntry() {
        return new Registration(emptyPrivateKey());
    }

    public Registration(PrivateKey privateKey, Certificate... chain) {
        this.privateKey = privateKey;
        this.chain = chain;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return getFirstCertificate().getPublicKey();
    }

    public Certificate[] getChain() {
        return chain;
    }

    public X509Certificate getFirstCertificate() {
        return (X509Certificate) chain[0];
    }

    public Date getCertificateNotBeforeDate() {
        return getFirstCertificate().getNotBefore();
    }

    private static PrivateKey emptyPrivateKey() {
        return new PrivateKey() {
            public String getAlgorithm() {
                return null;
            }

            public String getFormat() {
                return null;
            }

            public byte[] getEncoded() {
                return new byte[0];
            }
        };
    }

    public KeyStore.PrivateKeyEntry asKeyStoreEntry() {
        return new KeyStore.PrivateKeyEntry(privateKey, chain);
    }

    public String toJson() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("agentPrivateKey", serialize("RSA PRIVATE KEY", privateKey.getEncoded()));
        StringBuilder builder = new StringBuilder();
        for (Certificate c : chain) {
            try {
                builder.append(serialize("CERTIFICATE", c.getEncoded()));
            } catch (CertificateEncodingException e) {
                throw bomb(e);
            }
        }
        ret.put("agentCertificate", builder.toString());
        return new Gson().toJson(ret);
    }

    private String serialize(String type, byte[] data) {
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
