require 'java'

module OpenSSL
  class PKCS12
    java_import java.io.StringReader
    java_import java.io.StringBufferInputStream
    java_import java.security.cert.CertificateFactory
    java_import java.security.KeyStore
    java_import java.io.ByteArrayOutputStream
    java_import org.bouncycastle.openssl.PEMReader

    java.security.Security.add_provider(org.bouncycastle.jce.provider.BouncyCastleProvider.new)

    def self.create(pass, name, key, cert)
      pkcs12 = self.new(pass, name, key, cert)
      pkcs12.generate
      pkcs12
    end

    attr_reader :key, :certificate

    def initialize(pass, name, key, cert)
      @pass = pass
      @name = name
      @key = key
      @certificate = cert
    end

    def generate
      key_reader = StringReader.new(key.to_pem)
      key_pair = PEMReader.new(key_reader).read_object

      cert_input_stream = StringBufferInputStream.new(certificate.to_pem)
      certs = CertificateFactory.get_instance("X.509").generate_certificates(cert_input_stream)

      store = KeyStore.get_instance("PKCS12", "BC")
      store.load(nil, nil)
      store.set_key_entry(@name, key_pair.get_private, nil, certs.to_array(Java::java.security.cert.Certificate[certs.size].new))

      pkcs12_output_stream = ByteArrayOutputStream.new
      store.store(pkcs12_output_stream, @pass.to_java.to_char_array)

      @der = String.from_java_bytes(pkcs12_output_stream.to_byte_array)
    end

    def to_der
      @der
    end
  end
end
