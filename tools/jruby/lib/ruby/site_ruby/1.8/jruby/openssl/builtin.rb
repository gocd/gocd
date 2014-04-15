# this will load the gem and library only if it's available
require 'jruby/openssl/gem'

unless defined?(OpenSSL)
  module OpenSSL
    VERSION = "1.0.0"
    OPENSSL_VERSION = "OpenSSL 0.9.8b 04 May 2006 (JRuby fake)"
    OPENSSL_DUMMY = true

    class OpenSSLError < StandardError; end
    # These require the gem, so we present an error if they're accessed
    %w[
    ASN1
    BN
    Cipher
    Config
    Netscape
    PKCS7
    PKey
    Random
    SSL
    X509
    ].each {|c| autoload c, "jruby/openssl/autoloads/#{c.downcase}"}
    
    def self.determine_version
      require 'java'
      java.security.MessageDigest.getInstance("SHA224", PROVIDER);
      9469999
    rescue Exception
      9469952
    end
    OPENSSL_VERSION_NUMBER = determine_version

    require 'java'
    class HMACError < OpenSSLError; end
    class DigestError < OpenSSLError; end

    module Digest
      class Digest
        class << self
          def digest(name, data)
            self.new(name, data).digest
          end
          def hexdigest(name, data)
            self.new(name, data).hexdigest
          end
        end

        attr_reader :algorithm, :name, :data

        def initialize(name, data = nil)
          @name = name
          @data = ""
          create_digest
          update(data) if data
        end

        def initialize_copy(dig)
          initialize(dig.name, dig.data)
        end

        def update(d)
          @data << d
          @md.update(d.to_java_bytes)
          self
        end
        alias_method :<<, :update

        def digest
          @md.reset
          String.from_java_bytes @md.digest(@data.to_java_bytes)
        end

        def hexdigest
          digest.unpack("H*")[0]
        end
        alias_method :to_s, :hexdigest
        alias_method :inspect, :hexdigest

        def ==(oth)
          return false unless oth.kind_of? Digest
          self.algorithm == oth.algorithm && self.digest == oth.digest
        end

        def reset
          @md.reset
          @data = ""
        end

        def size
          @md.getDigestLength
        end

        private
        def create_digest
          @algorithm = case @name
          when "DSS"
            "SHA"
          when "DSS1"
            "SHA1"
          else
            @name
          end
          @md = java.security.MessageDigest.getInstance(@algorithm)
        end
      end

      begin
        old_verbose, $VERBOSE = $VERBOSE, nil # silence warnings
        # from openssl/digest.rb -- define the concrete digest classes
        alg = %w(DSS DSS1 MD2 MD4 MD5 MDC2 RIPEMD160 SHA SHA1)
        if ::OpenSSL::OPENSSL_VERSION_NUMBER > 0x00908000
          alg += %w(SHA224 SHA256 SHA384 SHA512)
        end
        alg.each{|name|
          klass = Class.new(Digest){
            define_method(:initialize){|*data|
              if data.length > 1
                raise ArgumentError,
                "wrong number of arguments (#{data.length} for 1)"
              end
              super(name, data.first)
            }
          }
          singleton = (class <<klass; self; end)
          singleton.class_eval{
            define_method(:digest){|data| Digest.digest(name, data) }
            define_method(:hexdigest){|data| Digest.hexdigest(name, data) }
          }
          const_set(name, klass)
        }
      ensure
        $VERBOSE = old_verbose
      end
    end

    class HMAC
      class << self
        def digest(dig, key, data)
          self.new(key, dig).update(data).digest
        end

        def hexdigest(dig, key, data)
          self.new(key, dig).update(data).hexdigest
        end
      end

      attr_reader :name, :key, :data

      def initialize(key, dig)
        @name = "HMAC" + dig.algorithm
        @key = key
        @data = ""
        create_mac
      end

      def initialize_copy(hmac)
        @name = hmac.name
        @key = hmac.key
        @data = hmac.data
        create_mac
      end

      def update(d)
        @data << d
        self
      end
      alias_method :<<, :update

      def digest
        @mac.reset
        String.from_java_bytes @mac.doFinal(@data.to_java_bytes)
      end

      def hexdigest
        digest.unpack("H*")[0]
      end

      alias_method :inspect, :hexdigest
      alias_method :to_s, :hexdigest

      private
      def create_mac
        @mac = javax.crypto.Mac.getInstance(@name)
        @mac.init(javax.crypto.spec.SecretKeySpec.new(@key.to_java_bytes, @name))
      end
    end
    warn %{JRuby limited openssl loaded. gem install jruby-openssl for full support.
http://wiki.jruby.org/wiki/JRuby_Builtin_OpenSSL}
  end
end