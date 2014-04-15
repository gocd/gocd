module OpenSSL

  #
  # Offers the same functionality as OpenSSL::PKCS5
  #
  module PKCS5
    module_function

    def pbkdf2_hmac_sha1(pass, salt, iter, keylen)
      Krypt::PBKDF2.new(Krypt::Digest::SHA1.new).generate(pass, salt, iter, keylen)
    end

    def pbkdf2_hmac(pass, salt, iter, keylen, digest)
      Krypt::PBKDF2.new(digest).generate(pass, salt, iter, keylen)
    end
  end unless defined? OpenSSL::PKCS5
end 
