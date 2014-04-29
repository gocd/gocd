require_relative 'base_codec'

module Krypt::Base64

  module Base64Impl #:nodoc:
    include Krypt::BaseCodec

    def compute_len(len, a, b)
      len -= @buf.size if @buf
      ret = a * len / b
      remainder = ret % a
      if remainder
        ret += a - remainder
      end
      ret
    end

    def compute_encode_read_len(len)
      compute_len(len, 3, 4)
    end

    def compute_decode_read_len(len)
      compute_len(len, 4, 3)
    end

    def generic_close
      if @write
        @io.write(Krypt::Base64.encode(@buf)) if @buf
      else
        raise Krypt::Base64::Base64Error.new("Remaining bytes in buffer") if @buf
      end
    end
  end

  private_constant :Base64Impl

  # Base64-encodes any data written or read from it in the process.
  #
  # === Example: Base64-encode data and write it to a file
  #
  #  f = File.open("b64", "wb")
  #  b64 = Krypt::Base64::Encoder.new(f)
  #  b64 << "one"
  #  b64 << "two"
  #  b64.close # => contents in file will be encoded
  #
  # === Example: Reading from a file and Base64-encoding the data
  #
  #  f = File.open("document", "rb")
  #  b64 = Krypt::Base64::Encoder.new(f)
  #  b64data = b64.read # => result is encoded
  #  b64.close
  #
  class Encoder < Krypt::IOFilter
    include Base64Impl

    #
    # call-seq:
    #    in.read([len=nil]) -> String or nil
    #
    # Reads from the underlying IO and Base64-encodes the data.
    # Please see IO#read for details. Note that in-place reading into
    # a buffer is not supported.
    #
    def read(len=nil)
      read_len = len ? compute_encode_read_len(len) : nil
      generic_read(len, read_len) { |data| Krypt::Base64.encode(data) }
    end

    #
    # call-seq:
    #    out.write(string) -> Integer
    #
    # Base64-encodes +string+ and writes it to the underlying IO.
    # Please see IO#write for further details.
    #
    def write(data)
      generic_write(data, 3) { |data| Krypt::Base64.encode(data) }
    end
    alias << write

    def close
      generic_close
      super
    end

  end
  
  # Base64-decodes any data written or read from it in the process.
  #
  # === Example: Reading and decoding Base64-encoded data from a file
  #
  #  f = File.open("b64", "rb")
  #  b64 = Krypt::Base64::Decoder.new(f)
  #  plain = b64.read # => result is decoded
  #  b64.close
  #
  # === Example: Writing to a file while Base64-decoding the data
  #
  #  f = File.open("document", "wb")
  #  b64 = Krypt::Base64::Decoder.new(f)
  #  b64data = ... #some Base64-encoded data
  #  b64 << b64data
  #  b64.close # => contents in file will be decoded
  #
  class Decoder < Krypt::IOFilter
    include Base64Impl

    #
    # call-seq:
    #    in.read([len=nil]) -> String or nil
    #
    # Reads from the underlying IO and Base64-decodes the data.
    # Please see IO#read for further details. Note that in-place reading into
    # a buffer is not supported.
    #
    def read(len=nil)
      read_len = len ? compute_decode_read_len(len) : nil
      generic_read(len, read_len) { |data| Krypt::Base64.decode(data) }
    end

    #
    # call-seq:
    #    out.write(string) -> Integer 
    #
    # Base64-decodes string and writes it to the underlying IO.
    # Please see IO#write for further details.
    #
    def write(data)
      generic_write(data, 4) { |data| Krypt::Base64.decode(data) }
    end
    alias << write

    def close
      generic_close
      super
    end
  end
  
end
