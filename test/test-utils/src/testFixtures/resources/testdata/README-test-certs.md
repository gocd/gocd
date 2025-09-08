Example certs/keys generated with smallstep CLI for testing

https://smallstep.com/docs/step-cli/reference/certificate/create/

Generate EC root cert
```shell
step certificate create "Example Root CA" root-ca-ec.crt root-ca-ec-key.pk1 \
  --profile root-ca --no-password --insecure
```

Generate a test EC key for a fake GoCD server reverse proxy
```shell
step certificate create localhost server-localhost-ec.crt server-localhost-ec-key.pk1 \
  --profile leaf --no-password  --insecure --not-after 63600h \
  --ca ./root-ca-ec.crt --ca-key ./root-ca-ec-key.pk1
```

Generate a test EC client cert for the agent, with a password-protected private key
```shell
step certificate create agent-client-cert-ec agent-client-cert-ec.crt agent-client-cert-ec-key.pk1 \
  --profile leaf --password-file agent-client-cert.pass --not-after 63600h \
  --ca ./root-ca-ec.crt --ca-key ./root-ca-ec-key.pk1
```

Generate a test RSA client cert for the agent, with a password-protected private key
```shell
step certificate create agent-client-cert-rsa agent-client-cert-rsa.crt agent-client-cert-rsa-key.pk1 \
  --profile leaf --kty RSA --size 4096 --password-file agent-client-cert.pass --not-after 63600h \
  --ca ./root-ca-ec.crt --ca-key ./root-ca-ec-key.pk1
```

Package the EC server certs as PKCS12 keystores
```shell
step certificate p12 root-ca-ec.p12 root-ca-ec.crt root-ca-ec-key.pk1 --password-file keystore.pass
step certificate p12 server-localhost-ec.p12 server-localhost-ec.crt server-localhost-ec-key.pk1 --password-file keystore.pass
```

Generate a separate EC agent cert without encrypted private key
```shell
step certificate create agent-client-cert-ec-nopass agent-client-cert-ec-nopass.crt agent-client-cert-ec-nopass-key.pk1 \
  --profile leaf --no-password --insecure --not-after 63600h \
  --ca ./root-ca-ec.crt --ca-key ./root-ca-ec-key.pk1
```

Generate a separate RSA agent cert without encrypted private key
```shell
step certificate create agent-client-cert-rsa-nopass agent-client-cert-rsa-nopass.crt agent-client-cert-rsa-nopass-key.pk1 \
  --profile leaf --kty RSA --size 4096 --no-password --insecure  --not-after 63600h \
  --ca ./root-ca-ec.crt --ca-key ./root-ca-ec-key.pk1
```

Create a chain of EC certs for testing
```shell
cat server-localhost-ec.crt root-ca-ec.crt > server-localhost-ec-chain.crt
```

Create PKCS8 versions of the agent private keys
```shell
step crypto key format agent-client-cert-ec-key.pk1 --pkcs8 --pem --password-file agent-client-cert.pass --out=agent-client-cert-ec-key.pk8
step crypto key format agent-client-cert-rsa-key.pk1 --pkcs8 --pem --password-file agent-client-cert.pass --out=agent-client-cert-rsa-key.pk8
step crypto key format agent-client-cert-ec-nopass-key.pk1 --pkcs8 --pem --no-password --insecure --out=agent-client-cert-ec-nopass-key.pk8
step crypto key format agent-client-cert-rsa-nopass-key.pk1 --pkcs8 --pem --no-password --insecure --out=agent-client-cert-rsa-nopass-key.pk8
```