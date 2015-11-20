require 'digest/md5'
require 'digest/sha1'
require 'digest/sha2'
require 'set'

module Sprockets
  # Internal: Hash functions and digest related utilities. Mixed into
  # Environment.
  module DigestUtils
    extend self

    # Internal: Default digest class.
    #
    # Returns a Digest::Base subclass.
    def digest_class
      Digest::SHA256
    end

    # Internal: Maps digest bytesize to the digest class.
    DIGEST_SIZES = {
      16 => Digest::MD5,
      20 => Digest::SHA1,
      32 => Digest::SHA256,
      48 => Digest::SHA384,
      64 => Digest::SHA512
    }

    # Internal: Detect digest class hash algorithm for digest bytes.
    #
    # While not elegant, all the supported digests have a unique bytesize.
    #
    # Returns Digest::Base or nil.
    def detect_digest_class(bytes)
      DIGEST_SIZES[bytes.bytesize]
    end

    # Internal: Generate a hexdigest for a nested JSON serializable object.
    #
    # This is used for generating cache keys, so its pretty important its
    # wicked fast. Microbenchmarks away!
    #
    # obj - A JSON serializable object.
    #
    # Returns a String digest of the object.
    def digest(obj)
      digest = digest_class.new
      queue  = [obj]

      while queue.length > 0
        obj = queue.shift
        klass = obj.class

        if klass == String
          digest << obj
        elsif klass == Symbol
          digest << 'Symbol'
          digest << obj.to_s
        elsif klass == Fixnum
          digest << 'Fixnum'
          digest << obj.to_s
        elsif klass == Bignum
          digest << 'Bignum'
          digest << obj.to_s
        elsif klass == TrueClass
          digest << 'TrueClass'
        elsif klass == FalseClass
          digest << 'FalseClass'
        elsif klass == NilClass
          digest << 'NilClass'.freeze
        elsif klass == Array
          digest << 'Array'
          queue.concat(obj)
        elsif klass == Hash
          digest << 'Hash'
          queue.concat(obj.sort)
        elsif klass == Set
          digest << 'Set'
          queue.concat(obj.to_a)
        elsif klass == Encoding
          digest << 'Encoding'
          digest << obj.name
        else
          raise TypeError, "couldn't digest #{klass}"
        end
      end

      digest.digest
    end

    # Internal: Pack a binary digest to a hex encoded string.
    #
    # bin - String bytes
    #
    # Returns hex String.
    def pack_hexdigest(bin)
      bin.unpack('H*').first
    end

    # Internal: Unpack a hex encoded digest string into binary bytes.
    #
    # hex - String hex
    #
    # Returns binary String.
    def unpack_hexdigest(hex)
      [hex].pack('H*')
    end

    # Internal: Pack a binary digest to a base64 encoded string.
    #
    # bin - String bytes
    #
    # Returns base64 String.
    def pack_base64digest(bin)
      [bin].pack('m0')
    end

    # Internal: Pack a binary digest to a urlsafe base64 encoded string.
    #
    # bin - String bytes
    #
    # Returns urlsafe base64 String.
    def pack_urlsafe_base64digest(bin)
      str = pack_base64digest(bin)
      str.tr!('+/'.freeze, '-_'.freeze)
      str.tr!('='.freeze, ''.freeze)
      str
    end

    # Internal: Maps digest class to the CSP hash algorithm name.
    HASH_ALGORITHMS = {
      Digest::SHA256 => 'sha256'.freeze,
      Digest::SHA384 => 'sha384'.freeze,
      Digest::SHA512 => 'sha512'.freeze
    }

    # Public: Generate hash for use in the `integrity` attribute of an asset tag
    # as per the subresource integrity specification.
    #
    # digest - The String byte digest of the asset content.
    #
    # Returns a String or nil if hash algorithm is incompatible.
    def integrity_uri(digest)
      case digest
      when Digest::Base
        digest_class = digest.class
        digest = digest.digest
      when String
        digest_class = DIGEST_SIZES[digest.bytesize]
      else
        raise TypeError, "unknown digest: #{digest.inspect}"
      end

      if hash_name = HASH_ALGORITHMS[digest_class]
        "#{hash_name}-#{pack_base64digest(digest)}"
      end
    end

    # Public: Generate hash for use in the `integrity` attribute of an asset tag
    # as per the subresource integrity specification.
    #
    # digest - The String hexbyte digest of the asset content.
    #
    # Returns a String or nil if hash algorithm is incompatible.
    def hexdigest_integrity_uri(hexdigest)
      integrity_uri(unpack_hexdigest(hexdigest))
    end
  end
end
