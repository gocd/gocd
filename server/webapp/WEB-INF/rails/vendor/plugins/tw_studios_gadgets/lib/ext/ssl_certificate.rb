if RUBY_PLATFORM =~ /java/
  class java::security::cert::X509Certificate

    java_import 'org.apache.commons.codec.binary.Hex'
    java_import 'java.security.MessageDigest'

    def md5_fingerprint
      digest("md5")
    end

    def sha1_fingerprint
      digest("sha1")
    end

    def serial_number_string
      self.serial_number.to_i.to_s(16).upcase
    end

    def issued_on
      to_utc(self.not_before)
    end

    def expires_on
      to_utc(self.not_after)
    end

    private
    def digest(algorithm)
      hex_bytes = Hex.encodeHex(MessageDigest.getInstance(algorithm).digest(self.encoded))
      colon_separated(java.lang.String.new(hex_bytes).to_s)
    end

    def colon_separated(string)
      string.upcase.scan(/.{2}/).join(':')
    end

    def to_utc(date)
      Time.parse(date.to_s).utc
    end

  end
end
