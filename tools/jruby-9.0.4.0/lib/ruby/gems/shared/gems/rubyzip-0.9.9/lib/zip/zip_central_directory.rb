module Zip
  class ZipCentralDirectory
    include Enumerable

    END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50
    MAX_END_OF_CENTRAL_DIRECTORY_STRUCTURE_SIZE = 65536 + 18
    STATIC_EOCD_SIZE = 22

    attr_reader :comment

    # Returns an Enumerable containing the entries.
    def entries
      @entrySet.entries
    end

    def initialize(entries = ZipEntrySet.new, comment = "")  #:nodoc:
      super()
      @entrySet = entries.kind_of?(ZipEntrySet) ? entries : ZipEntrySet.new(entries)
      @comment = comment
    end

    def write_to_stream(io)  #:nodoc:
      offset = io.tell
      @entrySet.each { |entry| entry.write_c_dir_entry(io) }
      write_e_o_c_d(io, offset)
    end

    def write_e_o_c_d(io, offset)  #:nodoc:
      tmp = [
        END_OF_CENTRAL_DIRECTORY_SIGNATURE,
        0                                  , # @numberOfThisDisk
        0                                  , # @numberOfDiskWithStartOfCDir
        @entrySet? @entrySet.size : 0      ,
        @entrySet? @entrySet.size : 0      ,
        cdir_size                          ,
        offset                             ,
        @comment ? @comment.length : 0
      ]
      io << tmp.pack('VvvvvVVv')
      io << @comment
    end
    
    private :write_e_o_c_d

    def cdir_size  #:nodoc:
      # does not include eocd
      @entrySet.inject(0) do |value, entry|
        entry.cdir_header_size + value
      end
    end
    
    private :cdir_size

    def read_e_o_c_d(io) #:nodoc:
      buf = get_e_o_c_d(io)
      @numberOfThisDisk                     = ZipEntry.read_zip_short(buf)
      @numberOfDiskWithStartOfCDir          = ZipEntry.read_zip_short(buf)
      @totalNumberOfEntriesInCDirOnThisDisk = ZipEntry.read_zip_short(buf)
      @size                                 = ZipEntry.read_zip_short(buf)
      @sizeInBytes                          = ZipEntry.read_zip_long(buf)
      @cdirOffset                           = ZipEntry.read_zip_long(buf)
      commentLength                         = ZipEntry.read_zip_short(buf)
      if commentLength <= 0
        @comment                            = buf.slice!(0, buf.size)
      else
        @comment                            = buf.read(commentLength)
      end
      raise ZipError, "Zip consistency problem while reading eocd structure" unless buf.size == 0
    end

    def read_central_directory_entries(io)  #:nodoc:
      begin
        io.seek(@cdirOffset, IO::SEEK_SET)
      rescue Errno::EINVAL
        raise ZipError, "Zip consistency problem while reading central directory entry"
      end
      @entrySet = ZipEntrySet.new
      @size.times do
        tmp = ZipEntry.read_c_dir_entry(io)
        @entrySet << tmp
      end
    end

    def read_from_stream(io)  #:nodoc:
      read_e_o_c_d(io)
      read_central_directory_entries(io)
    end

    def get_e_o_c_d(io)  #:nodoc:
      begin
        io.seek(-MAX_END_OF_CENTRAL_DIRECTORY_STRUCTURE_SIZE, IO::SEEK_END)
      rescue Errno::EINVAL
        io.seek(0, IO::SEEK_SET)
      end
      buf = io.read
      sigIndex = buf.rindex([END_OF_CENTRAL_DIRECTORY_SIGNATURE].pack('V'))
      raise ZipError, "Zip end of central directory signature not found" unless sigIndex
      buf = buf.slice!((sigIndex + 4)..(buf.bytesize))

      def buf.read(count)
        slice!(0, count)
      end

      buf
    end

    # For iterating over the entries.
    def each(&proc)
      @entrySet.each(&proc)
    end

    # Returns the number of entries in the central directory (and 
    # consequently in the zip archive).
    def size
      @entrySet.size
    end

    def ZipCentralDirectory.read_from_stream(io)  #:nodoc:
      cdir  = new
      cdir.read_from_stream(io)
      return cdir
    rescue ZipError
      return nil
    end

    def ==(other) #:nodoc:
      return false unless other.kind_of?(ZipCentralDirectory)
      @entrySet.entries.sort == other.entries.sort && comment == other.comment
    end
  end
end

  # Copyright (C) 2002, 2003 Thomas Sondergaard
  # rubyzip is free software; you can redistribute it and/or
  # modify it under the terms of the ruby license.
