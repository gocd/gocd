require 'java'

module OpenSSL
  class PKCS12
    class PKCS12Error < OpenSSLError
    end

    java_import java.io.StringReader
    java_import java.io.StringBufferInputStream
    java_import java.security.cert.CertificateFactory
    java_import java.security.cert.Certificate
    java_import java.security.KeyStore
    java_import java.io.ByteArrayOutputStream
    java_import org.bouncycastle.openssl.PEMReader

    java.security.Security.add_provider(org.bouncycastle.jce.provider.BouncyCastleProvider.new)

    def self.create(pass, name, key, cert, ca = nil)
      pkcs12 = self.new
      pkcs12.generate(pass, name, key, cert, ca)
      pkcs12
    end

    attr_reader :key, :certificate, :ca_certs

    def initialize(str = nil, pass = nil)
      if str
        if str.is_a?(File)
          file = File.open(str.path, "rb")
          @der = file.read
          file.close
        else
          @der = str
        end

        p12_input_stream = StringBufferInputStream.new(@der)

        store = KeyStore.get_instance("PKCS12")
        password = pass.nil? ? "" : pass
        begin
          store.load(p12_input_stream, password.to_java.to_char_array)
        rescue java.lang.Exception => e
          raise PKCS12Error, "Exception: #{e}"
        end

        aliases = store.aliases
        aliases.each { |alias_name|
          if store.is_key_entry(alias_name)
            begin
              java_certificate = store.get_certificate(alias_name)
            rescue java.lang.Exception => e
              raise PKCS12Error, "Exception: #{e}"
            end
            if java_certificate
              der = String.from_java_bytes(java_certificate.get_encoded)
              @certificate = OpenSSL::X509::Certificate.new(der)
            end

            begin
              java_key = store.get_key(alias_name, password.to_java.to_char_array)
            rescue java.lang.Exception => e
              raise PKCS12Error, "Exception: #{e}"
            end
            if java_key
              der = String.from_java_bytes(java_key.get_encoded)
              algorithm = java_key.get_algorithm
              if algorithm == "RSA"
                @key = OpenSSL::PKey::RSA.new(der)
              elsif algorithm == "DSA"
                @key = OpenSSL::PKey::DSA.new(der)
              elsif algorithm == "DH"
                @key = OpenSSL::PKey::DH.new(der)
              elsif algorithm == "EC"
                @key = OpenSSL::PKey::EC.new(der)
              else
                raise PKCS12Error, "Unknown key algorithm"
              end
            end

            @ca_certs = Array.new
            begin
              java_ca_certs = store.get_certificate_chain(alias_name)
            rescue java.lang.Exception => e
              raise PKCS12Error, "Exception #{e}"
            end
            if java_ca_certs
              java_ca_certs.each do |java_ca_cert|
                  der = String.from_java_bytes(java_ca_cert.get_encoded)
                  ruby_cert = OpenSSL::X509::Certificate.new(der)
                  if (ruby_cert.to_pem != @certificate.to_pem)
                    @ca_certs << ruby_cert
                  end
              end
            end
          end
          break
        }
      else
        @der = nil
      end
    end

    def generate(pass, alias_name, key, cert, ca = nil)
      @key = key
      @certificate = cert
      @ca_certs = ca

      key_reader = StringReader.new(key.to_pem)
      key_pair = PEMReader.new(key_reader).read_object

      certificates = cert.to_pem
      if ca
        ca.each { |ca_cert| 
          certificates << ca_cert.to_pem
        }
      end

      cert_input_stream = StringBufferInputStream.new(certificates)
      certs = CertificateFactory.get_instance("X.509").generate_certificates(cert_input_stream)

      store = KeyStore.get_instance("PKCS12", "BC")
      store.load(nil, nil)
      store.set_key_entry(alias_name, key_pair.get_private, nil, certs.to_array(Java::java.security.cert.Certificate[certs.size].new))

      pkcs12_output_stream = ByteArrayOutputStream.new
      password = pass.nil? ? "" : pass;
      begin
        store.store(pkcs12_output_stream, password.to_java.to_char_array)
      rescue java.lang.Exception => e
        raise PKCS12Error, "Exception: #{e}"
      end 

      @der = String.from_java_bytes(pkcs12_output_stream.to_byte_array)
    end

    def to_der
      @der
    end
  end
end
