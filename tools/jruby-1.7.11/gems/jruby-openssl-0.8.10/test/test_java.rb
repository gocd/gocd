$:.unshift File.join(File.dirname(__FILE__), '..', 'build_lib', 'mocha', 'lib')

require "test/unit"
require 'mocha'

if defined?(JRUBY_VERSION)
  require "java"
  $CLASSPATH << 'pkg/classes'
  $CLASSPATH << Dir["build_lib/bcprov-*"].first
  
  module PKCS7Test
    module ASN1
      OctetString = org.bouncycastle.asn1.DEROctetString
    end
    
    PKCS7 = org.jruby.ext.openssl.impl.PKCS7 unless defined?(PKCS7)
    Attribute = org.jruby.ext.openssl.impl.Attribute unless defined?(Attribute)
    CipherSpec = org.jruby.ext.openssl.impl.CipherSpec unless defined?(CipherSpec)
    Digest = org.jruby.ext.openssl.impl.Digest unless defined?(Digest)
    EncContent = org.jruby.ext.openssl.impl.EncContent unless defined?(EncContent)
    Encrypt = org.jruby.ext.openssl.impl.Encrypt unless defined?(Encrypt)
    Envelope = org.jruby.ext.openssl.impl.Envelope unless defined?(Envelope)
    IssuerAndSerial = org.jruby.ext.openssl.impl.IssuerAndSerial unless defined?(IssuerAndSerial)
    RecipInfo = org.jruby.ext.openssl.impl.RecipInfo unless defined?(RecipInfo)
    SignEnvelope = org.jruby.ext.openssl.impl.SignEnvelope unless defined?(SignEnvelope)
    Signed = org.jruby.ext.openssl.impl.Signed unless defined?(Signed)
    SMIME = org.jruby.ext.openssl.impl.SMIME unless defined?(SMIME)
    Mime = org.jruby.ext.openssl.impl.Mime unless defined?(Mime)
    MimeHeader = org.jruby.ext.openssl.impl.MimeHeader unless defined?(MimeHeader)
    MimeParam = org.jruby.ext.openssl.impl.MimeParam unless defined?(MimeParam)
    BIO = org.jruby.ext.openssl.impl.BIO unless defined?(BIO)
    PKCS7Exception = org.jruby.ext.openssl.impl.PKCS7Exception unless defined?(PKCS7Exception)
    ASN1Registry = org.jruby.ext.openssl.impl.ASN1Registry unless defined?(ASN1Registry)
    AlgorithmIdentifier = org.bouncycastle.asn1.x509.AlgorithmIdentifier unless defined?(AlgorithmIdentifier)
    SignerInfoWithPkey = org.jruby.ext.openssl.impl.SignerInfoWithPkey unless defined?(SignerInfoWithPkey)
    IssuerAndSerialNumber = org.bouncycastle.asn1.pkcs.IssuerAndSerialNumber unless defined?(IssuerAndSerialNumber)
    ASN1InputStream = org.bouncycastle.asn1.ASN1InputStream unless defined?(ASN1InputStream)
    X509AuxCertificate = org.jruby.ext.openssl.x509store.X509AuxCertificate unless defined?(X509AuxCertificate)
    
    ArrayList = java.util.ArrayList unless defined?(ArrayList)
    CertificateFactory = java.security.cert.CertificateFactory unless defined?(CertificateFactory)
    BCP = org.bouncycastle.jce.provider.BouncyCastleProvider unless defined?(BCP)
    ByteArrayInputStream = java.io.ByteArrayInputStream unless defined?(ByteArrayInputStream)
    BigInteger = java.math.BigInteger unless defined?(BigInteger)
    Cipher = javax.crypto.Cipher unless defined?(Cipher)

    ASN1Integer = org.bouncycastle.asn1.ASN1Integer
    DERSet = org.bouncycastle.asn1.DERSet
    DEROctetString = org.bouncycastle.asn1.DEROctetString 
    X500Name = org.bouncycastle.asn1.x500.X500Name
    
    
    MimeEnvelopedString = File::read(File.join(File.dirname(__FILE__), 'java', 'pkcs7_mime_enveloped.message'))
    MimeSignedString = File::read(File.join(File.dirname(__FILE__), 'java', 'pkcs7_mime_signed.message'))
    MultipartSignedString = File::read(File.join(File.dirname(__FILE__), 'java', 'pkcs7_multipart_signed.message'))

    X509CertString = <<CERT
-----BEGIN CERTIFICATE-----
MIICijCCAXKgAwIBAgIBAjANBgkqhkiG9w0BAQUFADA9MRMwEQYKCZImiZPyLGQB
GRYDb3JnMRkwFwYKCZImiZPyLGQBGRYJcnVieS1sYW5nMQswCQYDVQQDDAJDQTAe
Fw0wODA3MDgxOTE1NDZaFw0wODA3MDgxOTQ1NDZaMEQxEzARBgoJkiaJk/IsZAEZ
FgNvcmcxGTAXBgoJkiaJk/IsZAEZFglydWJ5LWxhbmcxEjAQBgNVBAMMCWxvY2Fs
aG9zdDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAy8LEsNRApz7U/j5DoB4X
BgO9Z8Atv5y/OVQRp0ag8Tqo1YewsWijxEWB7JOATwpBN267U4T1nPZIxxEEO7n/
WNa2ws9JWsjah8ssEBFSxZqdXKSLf0N4Hi7/GQ/aYoaMCiQ8jA4jegK2FJmXM71u
Pe+jFN/peeBOpRfyXxRFOYcCAwEAAaMSMBAwDgYDVR0PAQH/BAQDAgWgMA0GCSqG
SIb3DQEBBQUAA4IBAQCU879BALJIM9avHiuZ3WTjDy0UYP3ZG5wtuSqBSnD1k8pr
hXfRaga7mDj6EQaGUovImb+KrRi6mZc+zsx4rTxwBNJT9U8yiW2eYxmgcT9/qKrD
/1nz+e8NeUCCDY5UTUHGszZw5zLEDgDX2n3E/CDIZsoRSyq5vXq1jpfih/tSWanj
Y9uP/o8Dc7ZcRJOAX7NPu1bbZcbxEbZ8sMe5wZ5HNiAR6gnOrjz2Yyazb//PSskE
4flt/2h4pzGA0/ZHcnDjcoLdiLtInsqPOlVDLgqd/XqRYWtj84N4gw1iS9cHyrIZ
dqbS54IKvzElD+R0QVS2z6TIGJSpuSBnZ4yfuNuq
-----END CERTIFICATE-----
CERT

    X509CRLString = <<CRL
----BEGIN X509 CRL-----
MIIBlTB/AgEBMA0GCSqGSIb3DQEBBQUAMD0xEzARBgoJkiaJk/IsZAEZFgNvcmcx
GTAXBgoJkiaJk/IsZAEZFglydWJ5LWxhbmcxCzAJBgNVBAMMAkNBFw0wODA3MTgx
NzQxMjhaFw0wODA3MTgxODA4MDhaoA4wDDAKBgNVHRQEAwIBATANBgkqhkiG9w0B
AQUFAAOCAQEASJaj1keN+tMmsF3QmjH2RhbW/9rZAl4gjv+uQQqrcS2ByfkXLU1d
l/8rCHeT/XMoeU6xhQNHPP3uZBwfuuETcp65BMBcZFOUhUR0U5AaGhvSDS/+6EsP
zFdQgAagmThFdN5ei9guTLqWwN0ZyqiaHyevFJuk+L9qbKavaSeKqfJbU7Sj/Z3J
WLKoixvyj3N6W7evygH80lTvjZugmxJ1/AjICVSYr1hpHHd6EWq0b0YFrGFmg27R
WmsAXd0QV5UChfAJ2+Cz5U1bPszvIJGrzfAIoLxHv5rI5rseQzqZdPaFSe4Oehln
9qEYmsK3PS6bYoQol0cgj97Ep4olS8CulA==
-----END X509 CRL-----
CRL
    
    X509Cert = X509AuxCertificate.new(CertificateFactory.getInstance("X.509",BCP.new).generateCertificate(ByteArrayInputStream.new(X509CertString.to_java_bytes)))
    X509CRL = CertificateFactory.getInstance("X.509",BCP.new).generateCRL(ByteArrayInputStream.new(X509CRLString.to_java_bytes))
  end
  
  files = File.join(File.dirname(__FILE__), 'java', 'test_*.rb')
  Dir.glob(files).sort.each do |tc|
    require tc
  end
end
