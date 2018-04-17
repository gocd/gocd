require "securerandom"

module Digest
  module UUID
    DNS_NAMESPACE  = "k\xA7\xB8\x10\x9D\xAD\x11\xD1\x80\xB4\x00\xC0O\xD40\xC8" #:nodoc:
    URL_NAMESPACE  = "k\xA7\xB8\x11\x9D\xAD\x11\xD1\x80\xB4\x00\xC0O\xD40\xC8" #:nodoc:
    OID_NAMESPACE  = "k\xA7\xB8\x12\x9D\xAD\x11\xD1\x80\xB4\x00\xC0O\xD40\xC8" #:nodoc:
    X500_NAMESPACE = "k\xA7\xB8\x14\x9D\xAD\x11\xD1\x80\xB4\x00\xC0O\xD40\xC8" #:nodoc:

    # Generates a v5 non-random UUID (Universally Unique IDentifier).
    #
    # Using Digest::MD5 generates version 3 UUIDs; Digest::SHA1 generates version 5 UUIDs.
    # uuid_from_hash always generates the same UUID for a given name and namespace combination.
    #
    # See RFC 4122 for details of UUID at: http://www.ietf.org/rfc/rfc4122.txt
    def self.uuid_from_hash(hash_class, uuid_namespace, name)
      if hash_class == Digest::MD5
        version = 3
      elsif hash_class == Digest::SHA1
        version = 5
      else
        raise ArgumentError, "Expected Digest::SHA1 or Digest::MD5, got #{hash_class.name}."
      end

      hash = hash_class.new
      hash.update(uuid_namespace)
      hash.update(name)

      ary = hash.digest.unpack("NnnnnN")
      ary[2] = (ary[2] & 0x0FFF) | (version << 12)
      ary[3] = (ary[3] & 0x3FFF) | 0x8000

      "%08x-%04x-%04x-%04x-%04x%08x" % ary
    end

    # Convenience method for uuid_from_hash using Digest::MD5.
    def self.uuid_v3(uuid_namespace, name)
      uuid_from_hash(Digest::MD5, uuid_namespace, name)
    end

    # Convenience method for uuid_from_hash using Digest::SHA1.
    def self.uuid_v5(uuid_namespace, name)
      uuid_from_hash(Digest::SHA1, uuid_namespace, name)
    end

    # Convenience method for SecureRandom.uuid.
    def self.uuid_v4
      SecureRandom.uuid
    end
  end
end
