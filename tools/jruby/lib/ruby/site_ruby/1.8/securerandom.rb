# = Secure random number generator interface.
#
# This library is an interface for secure random number generator which is
# suitable for generating session key in HTTP cookies, etc.
#
# It supports following secure random number generators.
#
# * Java's java.security.SecureRandom.
#
# == Example
#
# # random hexadecimal string.
# p SecureRandom.hex(10) #=> "52750b30ffbc7de3b362"
# p SecureRandom.hex(10) #=> "92b15d6c8dc4beb5f559"
# p SecureRandom.hex(11) #=> "6aca1b5c58e4863e6b81b8"
# p SecureRandom.hex(12) #=> "94b2fff3e7fd9b9c391a2306"
# p SecureRandom.hex(13) #=> "39b290146bea6ce975c37cfc23"
# ...
#
# # random base64 string.
# p SecureRandom.base64(10) #=> "EcmTPZwWRAozdA=="
# p SecureRandom.base64(10) #=> "9b0nsevdwNuM/w=="
# p SecureRandom.base64(10) #=> "KO1nIU+p9DKxGg=="
# p SecureRandom.base64(11) #=> "l7XEiFja+8EKEtY="
# p SecureRandom.base64(12) #=> "7kJSM/MzBJI+75j8"
# p SecureRandom.base64(13) #=> "vKLJ0tXBHqQOuIcSIg=="
# ...
#
# # random binary string.
# p SecureRandom.random_bytes(10) #=> "\016\t{\370g\310pbr\301"
# p SecureRandom.random_bytes(10) #=> "\323U\030TO\234\357\020\a\337"
# ...

require 'java'

# Implements 1.8.7/1.9's SecureRandom class with java.security.SecureRandom.
class SecureRandom
  # SecureRandom.random_bytes generates a random binary string.
  #
  # The argument n specifies the length of the result string.
  #
  # If n is not specified, 16 is assumed.
  # It may be larger in future.
  #
  # If secure random number generator is not available,
  # NotImplementedError is raised.
  def self.random_bytes(n=nil)
    n ||= 16
    raise ArgumentError, "non-integer argument: #{n}" unless n.is_a?(Fixnum)
    raise ArgumentError, "negative argument: #{n}" if n < 0

    bytes = Java::byte[n].new
    java.security.SecureRandom.new.nextBytes(bytes)
    String.from_java_bytes bytes
  end

  # SecureRandom.hex generates a random hex string.
  #
  # The argument n specifies the length of the random length.
  # The length of the result string is twice of n.
  #
  # If n is not specified, 16 is assumed.
  # It may be larger in future.
  #
  # If secure random number generator is not available,
  # NotImplementedError is raised.
  def self.hex(n=nil)
    random_bytes(n).unpack("H*")[0]
  end

  # SecureRandom.base64 generates a random base64 string.
  #
  # The argument n specifies the length of the random length.
  # The length of the result string is about 4/3 of n.
  #
  # If n is not specified, 16 is assumed.
  # It may be larger in future.
  #
  # If secure random number generator is not available,
  # NotImplementedError is raised.
  def self.base64(n=nil)
    [random_bytes(n)].pack("m*").delete("\n")
  end

  # SecureRandom.random_number generates a random number.
  #
  # If an positive integer is given as n,
  # SecureRandom.random_number returns an integer:
  # 0 <= SecureRandom.random_number(n) < n.
  #
  # If 0 is given or an argument is not given,
  # SecureRandom.random_number returns an float:
  # 0.0 <= SecureRandom.random_number() < 1.0.
  def self.random_number(n=0)
    if 0 < n
      java.security.SecureRandom.new.nextInt(n)
    else
      java.security.SecureRandom.new.nextDouble
    end
  end
end