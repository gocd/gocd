# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

if RUBY_VERSION >= '1.9.0' # Required to properly load RubyZip under Ruby 1.9
  $LOADED_FEATURES.unshift 'ftools'
  require 'fileutils'

  def File.move(source, dest)
    FileUtils.move source, dest
  end

  def File.rm_rf(path)
    FileUtils.rm_rf path
  end
end

require 'zip/zip'
require 'zip/zipfilesystem'

module Zip #:nodoc:

  class ZipCentralDirectory #:nodoc:
    # Patch to add entries in alphabetical order.
    def write_to_stream(io)
      offset = io.tell
      @entrySet.sort { |a,b| a.name <=> b.name }.each { |entry| entry.write_c_dir_entry(io) }
      write_e_o_c_d(io, offset)
    end
  end


  class ZipEntry

    # :call-seq:
    #   exist() => boolean
    #
    # Returns true if this entry exists.
    def exist?()
      Zip::ZipFile.open(zipfile) { |zip| zip.file.exist?(@name) }
    end

    # :call-seq:
    #   empty?() => boolean
    #
    # Returns true if this entry is empty.
    def empty?()
      Zip::ZipFile.open(zipfile) { |zip| zip.file.read(@name) }.empty?
    end

    # :call-seq:
    #   contain(patterns*) => boolean
    #
    # Returns true if this ZIP file entry matches against all the arguments. An argument may be
    # a string or regular expression.
    def contain?(*patterns)
      content = Zip::ZipFile.open(zipfile) { |zip| zip.file.read(@name) }
      patterns.map { |pattern| Regexp === pattern ? pattern : Regexp.new(Regexp.escape(pattern.to_s)) }.
        all? { |pattern| content =~ pattern }
    end

    # Override of write_c_dir_entry to fix comments being set to a fixnum instead of string
    def write_c_dir_entry(io) #:nodoc:all
      case @fstype
        when FSTYPE_UNIX
          ft = nil
          case @ftype
            when :file
              ft = 010
              @unix_perms ||= 0644
            when :directory
              ft = 004
              @unix_perms ||= 0755
            when :symlink
              ft = 012
              @unix_perms ||= 0755
            else
              raise ZipInternalError, "unknown file type #{self.inspect}"
          end

          @externalFileAttributes = (ft << 12 | (@unix_perms & 07777)) << 16
      end

      io <<
        [0x02014b50,
         @version,                  # version of encoding software
         @fstype,                   # filesystem type
         10,                        # @versionNeededToExtract
         0,                         # @gp_flags
         @compression_method,
         @time.to_binary_dos_time,  # @lastModTime
         @time.to_binary_dos_date,  # @lastModDate
         @crc,
         @compressed_size,
         @size,
         @name ? @name.length : 0,
         @extra ? @extra.c_dir_length : 0,
         @comment ? comment.to_s.length : 0,
         0,                         # disk number start
         @internalFileAttributes,   # file type (binary=0, text=1)
         @externalFileAttributes,   # native filesystem attributes
         @localHeaderOffset,
         @name,
         @extra,
         @comment
      ].pack('VCCvvvvvVVVvvvvvVV')

      io << @name
      io << (@extra ? @extra.to_c_dir_bin : "")
      io << @comment
    end

    # Override write_c_dir_entry to fix comments being set to a fixnum instead of string
    def write_c_dir_entry(io) #:nodoc:all
      @comment = "" if @comment.nil? || @comment == -1  # Hack fix @comment being nil or fixnum -1

      case @fstype
        when FSTYPE_UNIX
          ft = nil
          case @ftype
            when :file
              ft = 010
              @unix_perms ||= 0644
            when :directory
              ft = 004
              @unix_perms ||= 0755
            when :symlink
              ft = 012
              @unix_perms ||= 0755
            else
              raise ZipInternalError, "unknown file type #{self.inspect}"
          end

          @externalFileAttributes = (ft << 12 | (@unix_perms & 07777)) << 16
      end

      io <<
        [0x02014b50,
         @version,                  # version of encoding software
         @fstype,                   # filesystem type
         10,                        # @versionNeededToExtract
         0,                         # @gp_flags
         @compression_method,
         @time.to_binary_dos_time,  # @lastModTime
         @time.to_binary_dos_date,  # @lastModDate
         @crc,
         @compressed_size,
         @size,
         @name ? @name.length : 0,
         @extra ? @extra.c_dir_length : 0,
         @comment ? @comment.length : 0,
         0,                         # disk number start
         @internalFileAttributes,   # file type (binary=0, text=1)
         @externalFileAttributes,   # native filesystem attributes
         @localHeaderOffset,
         @name,
         @extra,
         @comment].pack('VCCvvvvvVVVvvvvvVV')

      io << @name
      io << (@extra ? @extra.to_c_dir_bin : "")
      io << @comment

    end
  end

  class ZipEntrySet
    def <<(entry)
      @entrySet[entry.name.to_s] = entry if entry != nil
    end
  end
end
