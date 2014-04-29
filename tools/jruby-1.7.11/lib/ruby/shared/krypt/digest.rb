##
# Digest allows you to compute message digests (sometimes
# interchangeably called "hashes") of arbitrary data that are
# cryptographically secure, i.e. a Digest implements a secure one-way
# function.
#
# One-way functions offer some useful properties. E.g. given two
# distinct inputs the probability that both yield the same output
# is highly unlikely. Combined with the fact that every message digest
# algorithm has a fixed-length output of just a few bytes, digests are
# often used to create unique identifiers for arbitrary data. A common
# example is the creation of a unique id for binary documents that are
# stored in a database.
#
# Another useful characteristic of one-way functions (and thus the name)
# is that given a digest there is no indication about the original
# data that produced it, i.e. the only way to identify the original input
# is to "brute-force" through every possible combination of inputs.
#
# These characteristics make one-way functions also ideal companions
# for public key signature algorithms: instead of signing an entire
# document, first a hash of the document is produced with a considerably
# faster message digest algorithm and only the few bytes of its output
# need to be signed using the slower public key algorithm. To validate
# the integrity of a signed document, it suffices to re-compute the hash
# and verify that it is equal to that in the signature.
#
# Among the supported message digest algorithms are:
# * SHA1, SHA224, SHA256, SHA384 and SHA512
# * MD5
# * RIPEMD160
#
# For each of these algorithms, there is a convenient way to create
# instances of Digest using them, for example
#
#   digest = Krypt::Digest::SHA1.new
#
# === Creating Digest by name or by Object Identifier
#
# Each supported digest algorithm has an Object Identifier (OID) associated
# with it. A Digest can either be created by passing the string
# representation of the corresponding object identifier or by a string
# representation of the algorithm name.
#
# For example, the OBJECT IDENTIFIER for SHA-1 is 1.3.14.3.2.26, so it can
# be instantiated like this:
# 
#   d = Krypt::Digest.new("1.3.14.3.2.26")
#   d = Krypt::Digest.new("SHA1")
#   d = Krypt::Digest.new("sha1")
#
# Algorithm names may either be all upper- or all lowercase, hyphens are
# generally stripped: for instance SHA-1 becomes "SHA1", RIPEMD-160 
# becomes "RIPEMD160".
#
# "Breaking" a message digest algorithm means defying its one-way
# function characteristics, i.e. producing a collision or finding a way
# to get to the original data by means that are more efficient than
# brute-forcing etc. Older digest algorithms can be considered broken
# in this sense, even the very popular MD5 and SHA1 algorithms. Should
# security be your highest concern, then you should probably rely on
# SHA224, SHA256, SHA384 or SHA512.
#
# === Hashing a file
#
#   data = File.read('document')
#   sha256 = Krypt::Digest::SHA256.new
#   digest = sha256.digest(data)
#
# === Hashing several pieces of data at once
#
#   data1 = File.read('file1')
#   data2 = File.read('file2')
#   data3 = File.read('file3')
#   sha256 = Krypt::Digest::SHA256.new
#   sha256 << data1
#   sha256 << data2
#   sha256 << data3
#   digest = sha256.digest
#
# === Reuse a Digest instance
#
#   data1 = File.read('file1')
#   sha256 = Krypt::Digest::SHA256.new
#   digest1 = sha256.digest(data1)
#
#   data2 = File.read('file2')
#   sha256.reset
#   digest2 = sha256.digest(data2)
#
module Krypt::Digest

  ##
  # Raised whenever a problem with digests occurs.
  #
  class DigestError < Krypt::Error; end

  def self.new(name_or_oid, provider=nil)
    receiver = provider ? provider : Krypt::Provider
    f = ->(_) { new_service(Krypt::Digest, name_or_oid) }
    receiver.instance_eval(&f)
  end

  %w(SHA1 SHA224 SHA256 SHA384 SHA512 RIPEMD160 MD5).each do |alg|
    mod = Module.new do
      define_singleton_method(:new) { Krypt::Digest.new(alg) }
    end
    const_set(alg, mod)
  end

end

