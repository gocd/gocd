# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Digest

  NO_MESSAGE = Object.new

  class Class

    def self.file(file)
      digest = new
      digest.update File.read(file)
      digest
    end

    def self.digest(message)
      digest = new
      digest.update message
      digest.digest
    end

    def self.hexdigest(message)
      digest = new
      digest.update message
      digest.hexdigest
    end

    def update(message)
      Truffle::Digest.update @digest, message
    end

    alias_method :<<, :update

    def reset
      Truffle::Digest.reset @digest
    end

    def digest(message=NO_MESSAGE)
      if NO_MESSAGE == message
        Truffle::Digest.digest @digest
      else
        reset
        update message
        digest!
      end
    end

    def hexdigest(message=NO_MESSAGE)
      Digest.hexencode(digest(message))
    end

    alias_method :to_s, :hexdigest
    alias_method :to_str, :hexdigest

    def digest!
      digested = digest
      reset
      digested
    end

    def hexdigest!
      digested = hexdigest
      reset
      digested
    end

    def digest_length
      Truffle::Digest.digest_length @digest
    end

    alias_method :size, :digest_length
    alias_method :length, :digest_length

    def ==(other)
      hexdigest == other.to_str
    end

    def inspect
      "#<#{self.class.name}: #{hexdigest}>"
    end

  end

  class MD5 < Class

    def initialize
      @digest = Truffle::Digest.md5
    end

    def block_length
      64
    end

  end

  class SHA1 < Class

    def initialize
      @digest = Truffle::Digest.sha1
    end

    def block_length
      64
    end

  end

  class SHA256 < Class

    def initialize
      @digest = Truffle::Digest.sha256
    end

    def block_length
      64
    end

  end

  class SHA384 < Class

    def initialize
      @digest = Truffle::Digest.sha384
    end

    def block_length
      128
    end

  end

  class SHA512 < Class

    def initialize
      @digest = Truffle::Digest.sha512
    end

    def block_length
      128
    end

  end

  def self.hexencode(message)
    StringValue(message).unpack('H*').first
  end

end

require 'digest/sha2'
