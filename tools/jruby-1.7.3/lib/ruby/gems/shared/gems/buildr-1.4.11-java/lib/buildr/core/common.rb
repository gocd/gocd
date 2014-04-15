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

module Buildr

  # :call-seq:
  #   struct(hash) => Struct
  #
  # Convenience method for creating an anonymous Struct.
  #
  # For example:
  #   COMMONS             = struct(
  #     :collections      =>'commons-collections:commons-collections:jar:3.1',
  #     :lang             =>'commons-lang:commons-lang:jar:2.1',
  #     :logging          =>'commons-logging:commons-logging:jar:1.0.3',
  #   )
  #
  #   compile.with COMMONS.logging
  def struct(hash)
    Struct.new(nil, *hash.keys).new(*hash.values)
  end

  # :call-seq:
  #   write(name, content)
  #   write(name) { ... }
  #
  # Write the contents into a file. The second form calls the block and writes the result.
  #
  # For example:
  #   write 'TIMESTAMP', Time.now
  #   write('TIMESTAMP') { Time.now }
  #
  # Yields to the block before writing the file, so you can chain read and write together.
  # For example:
  #   write('README') { read('README').sub("${build}", Time.now) }
  def write(name, content = nil)
    mkpath File.dirname(name)
    content = yield if block_given?
    File.open(name.to_s, 'wb') { |file| file.write content.to_s }
    content.to_s
  end

  # :call-seq:
  #   read(args) => string
  #   read(args) { |string| ... } => result
  #
  # Reads and returns the contents of a file. The second form yields to the block and returns
  # the result of the block. The args passed to read are passed on to File.open.
  #
  # For example:
  #   puts read('README')
  #   read('README') { |text| puts text }
  def read(*args)
    args[0] = args[0].to_s
    contents = File.open(*args) { |f| f.read }
    if block_given?
      yield contents
    else
      contents
    end
  end

  # :call-seq:
  #    download(url_or_uri) => task
  #    download(path=>url_or_uri) =>task
  #
  # Create a task that will download a file from a URL.
  #
  # Takes a single argument, a hash with one pair. The key is the file being
  # created, the value if the URL to download. The task executes only if the
  # file does not exist; the URL is not checked for updates.
  #
  # The task will show download progress on the console; if there are MD5/SHA1
  # checksums on the server it will verify the download before saving it.
  #
  # For example:
  #   download 'image.jpg'=>'http://example.com/theme/image.jpg'
  def download(args)
    args = URI.parse(args) if String === args
    if URI === args
      # Given only a download URL, download into a temporary file.
      # You can infer the file from task name.
      temp = Tempfile.open(File.basename(args.to_s))
      file(temp.path).tap do |task|
        # Since temporary file exists, force a download.
        class << task ; def needed? ; true ; end ; end
        task.sources << args
        task.enhance { args.download temp }
      end
    else
      # Download to a file created by the task.
      fail unless args.keys.size == 1
      uri = URI.parse(args.values.first.to_s)
      file(args.keys.first.to_s).tap do |task|
        task.sources << uri
        task.enhance { uri.download task.name }
      end
    end

  end

  # A file task that concatenates all its prerequisites to create a new file.
  #
  # For example:
  #   concat("master.sql"=>["users.sql", "orders.sql", reports.sql"]
  #
  # See also Buildr#concat.
  class ConcatTask < Rake::FileTask
    def initialize(*args) #:nodoc:
      super
      enhance do |task|
        content = prerequisites.inject("") do |content, prereq|
          content << File.read(prereq.to_s) if File.exists?(prereq) && !File.directory?(prereq)
          content
        end
        File.open(task.name, "wb") { |file| file.write content }
      end
    end
  end

  # :call-seq:
  #    concat(target=>files) => task
  #
  # Creates and returns a file task that concatenates all its prerequisites to create
  # a new file. See #ConcatTask.
  #
  # For example:
  #   concat("master.sql"=>["users.sql", "orders.sql", reports.sql"]
  def concat(args)
    file, arg_names, deps = Buildr.application.resolve_args([args])
    ConcatTask.define_task(File.expand_path(file)=>deps)
  end

end
