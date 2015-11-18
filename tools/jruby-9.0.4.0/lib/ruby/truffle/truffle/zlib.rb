# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Zlib

  BINARY              = 0
  ASCII               = 1
  UNKNOWN             = 2

  DEF_MEM_LEVEL       = 8
  MAX_MEM_LEVEL       = 9

  OS_MSDOS            = 0
  OS_AMIGA            = 1
  OS_VMS              = 2
  OS_CODE             = 3
  OS_UNIX             = 3
  OS_VMCMS            = 4
  OS_ATARI            = 5
  OS_OS2              = 6
  OS_MACOS            = 7
  OS_ZSYSTEM          = 8
  OS_CPM              = 9
  OS_TOPS20           = 10
  OS_WIN32            = 11
  OS_QDOS             = 12
  OS_RISCOS           = 13
  OS_UNKNOWN          = 255

  DEFAULT_STRATEGY    = 0
  FILTERED            = 1
  HUFFMAN_ONLY        = 2

  NO_FLUSH            = 0
  SYNC_FUSH           = 2
  FULL_FLUSH          = 3
  FINISH              = 4

  NO_COMPRESSION      =  0
  BEST_SPEED          =  1
  BEST_COMPRESSION    =  9
  DEFAULT_COMPRESSION = -1

  MAX_WBITS           = 15

  def self.crc32(*args)
    Truffle::Zlib.crc32(*args)
  end

  def self.adler32(*args)
    Truffle::Zlib.adler32(*args)
  end

  module Deflate

    def self.deflate(message, level=DEFAULT_COMPRESSION)
      Truffle::Zlib.deflate(message, level)
    end

  end

  module Inflate

    def self.inflate(message)
      Truffle::Zlib.inflate(message)
    end

  end

  class GzipFile
  end

  class GzipWriter < GzipFile

    def initialize(io, level = nil, strategy = nil, options = {})
      raise "not supported"
    end

    def self.open(filename, level=nil, strategy=nil)
      raise "not supported"
    end

    def <<(p1)
      raise "not supported"
    end

    def comment=(p1)
      raise "not supported"
    end

    def flush(flush=nil)
      raise "not supported"
    end

    def mtime=(p1)
      raise "not supported"
    end

    def orig_name=(p1)
      raise "not supported"
    end

    def pos()
      raise "not supported"
    end

    def print(*args)
      raise "not supported"
    end

    def printf(*args)
      raise "not supported"
    end

    def putc(p1)
      raise "not supported"
    end

    def puts(*args)
      raise "not supported"
    end

    def tell()
      raise "not supported"
    end

    def write(p1)
      raise "not supported"
    end

  end

  class GzipReader < GzipFile

    def initialize(io, options = {})
      raise "not supported"
    end

    def self.open(filename)
      raise "not supported"
    end

    def bytes()
      raise "not supported"
    end

    def each(*args)
      raise "not supported"
    end

    def each_byte()
      raise "not supported"
    end

    def each_char()
      raise "not supported"
    end

    def each_line(*args)
      raise "not supported"
    end

    def eof()
      raise "not supported"
    end

    def eof?()
      raise "not supported"
    end

    def getbyte()
      raise "not supported"
    end

    def getc()
      raise "not supported"
    end

    def gets(*args)
      raise "not supported"
    end

    def lineno()
      raise "not supported"
    end

    def lineno=(p1)
      raise "not supported"
    end

    def lines(*args)
      raise "not supported"
    end

    def pos()
      raise "not supported"
    end

    def read(p1 = v1)
      raise "not supported"
    end

    def readbyte()
      raise "not supported"
    end

    def readchar()
      raise "not supported"
    end

    def readline(*args)
      raise "not supported"
    end

    def readlines(*args)
      raise "not supported"
    end

    def readpartial(maxlen , outbuf)
      raise "not supported"
    end

    def rewind()
      raise "not supported"
    end

    def tell()
      raise "not supported"
    end

    def ungetbyte(p1)
      raise "not supported"
    end

    def ungetc(p1)
      raise "not supported"
    end

    def unused()
      raise "not supported"
    end

  end

end
