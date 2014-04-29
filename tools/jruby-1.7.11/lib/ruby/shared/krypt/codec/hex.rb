require_relative 'base_codec'

module Krypt::Hex

  module HexImpl #:nodoc:
    include Krypt::BaseCodec

    def compute_encode_read_len(len)
      len
    end

    def compute_decode_read_len(len)
      len * 2
    end

    def generic_close
      raise Krypt::Hex::HexError.new("Remaining bytes in buffer") if @buf
    end
  end

  private_constant :HexImpl


  # Hex-encodes any data written or read from it in the process.
  #
  # === Example: Hex-encode data and write it to a file
  #
  #  f = File.open("hex", "wb")
  #  hex = Krypt::Hex::Encoder.new(f)
  #  hex << "one"
  #  hex << "two"
  #  hex.close # => contents in file will be encoded
  #
  # === Example: Reading from a file and hex-encoding the data
  #
  #  f = File.open("document", "rb")
  #  hex = Krypt::Hex::Encoder.new(f)
  #  hexdata = hex.read # => result is encoded
  #  hex.close
  #
  class Encoder < Krypt::IOFilter
    include HexImpl

    #
    # call-seq:
    #    in.read([len=nil]) -> String or nil
    #
    # Reads from the underlying IO and hex-encodes the data.
    # Please see IO#read for details. Note that in-place reading into
    # a buffer is not supported.
    #
    def read(len=nil)
      read_len = len ? compute_encode_read_len(len) : nil
      generic_read(len, read_len) { |data| Krypt::Hex.encode(data) }
    end

    #
    # call-seq:
    #    out.write(string) -> Integer
    #
    # Hex-encodes +string+ and writes it to the underlying IO.
    # Please see IO#write for further details.
    #
    def write(data)
      generic_write(data, 1) { |data| Krypt::Hex.encode(data) }
    end
    alias << write

  end
  
  # Hex-decodes any data written or read from it in the process.
  #
  # === Example: Reading and decoding hex-encoded data from a file
  #
  #  f = File.open("hex", "rb")
  #  hex = Krypt::Hex::Decoder.new(f)
  #  plain = hex.read # => result is decoded
  #  hex.close
  #
  # === Example: Writing to a file while hex-decoding the data
  #
  #  f = File.open("document", "wb")
  #  hex = Krypt::Hex::Decoder.new(f)
  #  hexdata = ... #some hex-encoded data
  #  hex << hexdata
  #  hex.close # => contents in file will be decoded
  #
  class Decoder < Krypt::IOFilter
    include HexImpl

    #
    # call-seq:
    #    in.read([len=nil], [buf=nil]) -> String or nil
    #
    # Reads from the underlying IO and hex-decodes the data.
    # Please see IO#read for further details. Note that in-place reading into
    # a buffer is not supported.
    #
    def read(len=nil)
      read_len = len ? compute_decode_read_len(len) : nil
      generic_read(len, read_len) { |data| Krypt::Hex.decode(data) }
    end

    #
    # call-seq:
    #    out.write(string) -> Integer 
    #
    # Hex-decodes string and writes it to the underlying IO.
    # Please see IO#write for further details.
    #
    def write(data)
      generic_write(data, 2) { |data| Krypt::Hex.decode(data) }
    end
    alias << write

    def close
      generic_close
      super
    end

  end
end
