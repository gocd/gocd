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


$LOADED_FEATURES.unshift 'ftools' if RUBY_VERSION >= '1.9.0' # Required to properly load RubyZip under Ruby 1.9
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

  end
end
