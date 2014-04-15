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

autoload :Archive, 'archive/tar/minitar'
autoload :Zlib, 'zlib'

module Buildr

  # The TarTask creates a new Tar file. You can include any number of files and and directories,
  # use exclusion patterns, and include files into specific directories.
  #
  # To create a GZipped Tar, either set the gzip option to true, or use the .tgz or .gz suffix.
  #
  # For example:
  #   tar("test.tgz").tap do |task|
  #     task.include "srcs"
  #     task.include "README", "LICENSE"
  #   end
  #
  # See Buildr#tar and ArchiveTask.
  class TarTask < ArchiveTask

    # To create a GZipped Tar, either set this option to true, or use the .tgz/.gz suffix.
    attr_accessor :gzip
    # Permission mode for files contained in the Tar.  Defaults to 0755.
    attr_accessor :mode

    def initialize(*args, &block) #:nodoc:
      super
      self.gzip = name =~ /\.t?gz$/
      self.mode = '0755'
    end

    # :call-seq:
    #   entry(name) => Entry
    #
    # Returns a Tar file entry. You can use this to check if the entry exists and its contents,
    # for example:
    #   package(:tar).entry("src/LICENSE").should contain(/Apache Software License/)
    def entry(entry_name)
      Buildr::TarEntry.new(self, entry_name)
    end

    def entries() #:nodoc:
      tar_entries = nil
      with_uncompressed_tar { |tar| tar_entries = tar.entries }
      tar_entries
    end

    # :call-seq:
    #   with_uncompressed_tar { |tar_entries| ... }
    #
    # Yields an Archive::Tar::Minitar::Input object to the provided block.
    # Opening, closing and Gzip-decompressing is automatically taken care of.
    def with_uncompressed_tar &block
      if gzip
        Zlib::GzipReader.open(name) { |tar| Archive::Tar::Minitar.open(tar, &block) }
      else
        Archive::Tar::Minitar.open(name, &block)
      end
    end

  private

    def create_from(file_map)
      if gzip
        StringIO.new.tap do |io|
          create_tar io, file_map
          io.seek 0
          Zlib::GzipWriter.open(name) { |gzip| gzip.write io.read }
        end
      else
        File.open(name, 'wb') { |file| create_tar file, file_map }
      end
    end

    def create_tar(out, file_map)
      Archive::Tar::Minitar::Writer.open(out) do |tar|
        options = { :mode=>mode || '0755', :mtime=>Time.now }

        file_map.each do |path, content|
          if content.respond_to?(:call)
            tar.add_file(path, options) { |os, opts| content.call os }
          elsif content.nil?
          elsif File.directory?(content.to_s)
            stat = File.stat(content.to_s)
            tar.mkdir(path, options.merge(:mode=>stat.mode, :mtime=>stat.mtime, :uid=>stat.uid, :gid=>stat.gid))
          else
            File.open content.to_s, 'rb' do |is|
              tar.add_file path, options.merge(:mode=>is.stat.mode, :mtime=>is.stat.mtime, :uid=>is.stat.uid, :gid=>is.stat.gid) do |os, opts|
                while data = is.read(4096)
                  os.write(data)
                end
              end
            end
          end
        end
      end
    end

  end


  class TarEntry #:nodoc:

    def initialize(tar_task, entry_name)
      @tar_task = tar_task
      @entry_name = entry_name
    end

    # :call-seq:
    #   contain?(*patterns) => boolean
    #
    # Returns true if this Tar file entry matches against all the arguments. An argument may be
    # a string or regular expression.
    def contain?(*patterns)
      content = read_content_from_tar
      patterns.map { |pattern| Regexp === pattern ? pattern : Regexp.new(Regexp.escape(pattern.to_s)) }.
        all? { |pattern| content =~ pattern }
    end

    # :call-seq:
    #   empty?() => boolean
    #
    # Returns true if this entry is empty.
    def empty?()
      read_content_from_tar.nil?
    end

    # :call-seq:
    #   exist() => boolean
    #
    # Returns true if this entry exists.
    def exist?()
      exist = false
      @tar_task.with_uncompressed_tar { |tar| exist = tar.any? { |entry| entry.name == @entry_name } }
      exist
    end

    def to_s #:nodoc:
      @entry_name
    end

    private

    def read_content_from_tar
      content = Errno::ENOENT.new("No such file or directory - #{@entry_name}")
      @tar_task.with_uncompressed_tar do |tar|
        content = tar.inject(content) { |content, entry| entry.name == @entry_name ? entry.read : content }
      end
      raise content if Exception === content
      content
    end
  end

end


# :call-seq:
#    tar(file) => TarTask
#
# The TarTask creates a new Tar file. You can include any number of files and
# and directories, use exclusion patterns, and include files into specific
# directories.
#
# To create a GZipped Tar, either set the gzip option to true, or use the .tgz or .gz suffix.
#
# For example:
#   tar("test.tgz").tap do |tgz|
#     tgz.include "srcs"
#     tgz.include "README", "LICENSE"
#   end
def tar(file)
  TarTask.define_task(file)
end
