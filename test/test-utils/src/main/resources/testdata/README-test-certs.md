Example certs/keys generated with smallstep CLI for testing

https://smallstep.com/docs/step-cli/reference/certificate/create/

Generate root cert
```shell
step certificate create "Example Root CA" root-ca.crt root-ca.key \
  --profile root-ca --no-password --insecure
```

Generate a test key for a fake GoCD server reverse proxy
```shell
step certificate create localhost server-localhost.crt server-localhost.key \
  --profile leaf --no-password  --insecure --not-after 63600h \
  --ca ./root-ca.crt --ca-key ./root-ca.key
```

Generate a test client cert for the agent, with a password-protected private key
```shell
step certificate create agent-client-cert agent-client-cert.crt agent-client-cert.key \
  --profile leaf --password-file agent-cert-key.pass --not-after 63600h \
  --ca ./root-ca.crt --ca-key ./root-ca.key
```

Package the certs as PKCS12 keystores
```shell
step certificate p12 root-ca.p12 root-ca.crt root-ca.key --password-file keystore.pass
step certificate p12 server-localhost.p12 server-localhost.crt server-localhost.key --password-file keystore.pass
```

Generate a separate agent cert without encrypted private key
```shell
step certificate create agent-client-cert-nopass agent-client-cert-nopass.crt agent-client-cert-nopass.key \
  --profile leaf --no-password --insecure --not-after 63600h \
  --ca ./root-ca.crt --ca-key ./root-ca.key
```

Create a chain of certs for testing
```shell
cat server-localhost.crt root-ca.crt > server-localhost-chain.crt
```