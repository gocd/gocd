module Zip
  module NullEncryption
    def header_bytesize
      0
    end

    def gp_flags
      0
    end
  end

  class NullEncrypter < Encrypter
    include NullEncryption

    def header(mtime)
      ''
    end

    def encrypt(data)
      data
    end

    def data_descriptor(crc32, compressed_size, uncomprssed_size)
      ''
    end

    def reset!
    end
  end

  class NullDecrypter < Decrypter
    include NullEncryption

    def decrypt(data)
      data
    end

    def reset!(header)
    end
  end
end

# Copyright (C) 2002, 2003 Thomas Sondergaard
# rubyzip is free software; you can redistribute it and/or
# modify it under the terms of the ruby license.
