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

module Buildr #:nodoc:

  # The ZipTask creates a new Zip file. You can include any number of files and and directories,
  # use exclusion patterns, and include files into specific directories.
  #
  # For example:
  #   zip('test.zip').tap do |task|
  #     task.include 'srcs'
  #     task.include 'README', 'LICENSE'
  #   end
  #
  # See Buildr#zip and ArchiveTask.
  class ZipTask < ArchiveTask

    # Compression level for this Zip.
    attr_accessor :compression_level

    def initialize(*args) #:nodoc:
      self.compression_level = Zlib::DEFAULT_COMPRESSION
      super
    end

    # :call-seq:
    #   entry(name) => Entry
    #
    # Returns a ZIP file entry. You can use this to check if the entry exists and its contents,
    # for example:
    #   package(:jar).entry("META-INF/LICENSE").should contain(/Apache Software License/)
    def entry(entry_name)
      ::Zip::ZipEntry.new(name, entry_name)
    end

    def entries #:nodoc:
      @entries ||= Zip::ZipFile.open(name) { |zip| zip.entries }
    end

  private

    def create_from(file_map)
      Zip::ZipOutputStream.open name do |zip|
        seen = {}
        mkpath = lambda do |dir|
          dirname = (dir[-1..-1] =~ /\/$/) ? dir : dir + '/'
          unless dir == '.' || seen[dirname]
            mkpath.call File.dirname(dirname)
            zip.put_next_entry(dirname, compression_level)
            seen[dirname] = true
          end
        end

        file_map.each do |path, content|
          warn "Warning:  Path in zipfile #{name} contains backslash: #{path}" if path =~ /\\/
          mkpath.call File.dirname(path)
          if content.respond_to?(:call)
            entry = zip.put_next_entry(path, compression_level)
            entry.unix_perms = content.mode & 07777 if content.respond_to?(:mode)
            content.call zip
          elsif content.nil? || File.directory?(content.to_s)
            mkpath.call path
          else
            entry = zip.put_next_entry(path, compression_level)
            File.open content.to_s, 'rb' do |is|
              entry.unix_perms = is.stat.mode & 07777
              while data = is.read(4096)
                zip << data
              end
            end
          end
        end
      end
    end

  end


  # :call-seq:
  #    zip(file) => ZipTask
  #
  # The ZipTask creates a new Zip file. You can include any number of files and
  # and directories, use exclusion patterns, and include files into specific
  # directories.
  #
  # For example:
  #   zip('test.zip').tap do |task|
  #     task.include 'srcs'
  #     task.include 'README', 'LICENSE'
  #   end
  def zip(file)
    ZipTask.define_task(file)
  end


  # An object for unzipping/untarring a file into a target directory. You can tell it to include
  # or exclude only specific files and directories, and also to map files from particular
  # paths inside the zip file into the target directory. Once ready, call #extract.
  #
  # Usually it is more convenient to create a file task for extracting the zip file
  # (see #unzip) and pass this object as a prerequisite to other tasks.
  #
  # See Buildr#unzip.
  class Unzip

    # The zip file to extract.
    attr_accessor :zip_file
    # The target directory to extract to.
    attr_accessor :target

    # Initialize with hash argument of the form target=>zip_file.
    def initialize(args)
      @target, arg_names, zip_file = Buildr.application.resolve_args([args])
      @zip_file = zip_file.first
      @paths = {}
    end

    # :call-seq:
    #   extract
    #
    # Extract the zip/tgz file into the target directory.
    #
    # You can call this method directly. However, if you are using the #unzip method,
    # it creates a file task for the target directory: use that task instead as a
    # prerequisite. For example:
    #   build unzip(dir=>zip_file)
    # Or:
    #   unzip(dir=>zip_file).target.invoke
    def extract
      # If no paths specified, then no include/exclude patterns
      # specified. Nothing will happen unless we include all files.
      if @paths.empty?
        @paths[nil] = FromPath.new(self, nil)
      end

      # Otherwise, empty unzip creates target as a file when touching.
      mkpath target.to_s
      if zip_file.to_s.match /\.t?gz$/
        #un-tar.gz
        Zlib::GzipReader.open(zip_file.to_s) { |tar|
          Archive::Tar::Minitar::Input.open(tar) do |inp|
            inp.each do |tar_entry|
              @paths.each do |path, patterns|
                patterns.map([tar_entry]).each do |dest, entry|
                  next if entry.directory?
                  dest = File.expand_path(dest, target.to_s)
                  trace "Extracting #{dest}"
                  mkpath File.dirname(dest) rescue nil
                  File.open(dest, 'wb', entry.mode) {|f| f.write entry.read}
                  File.chmod(entry.mode, dest)
                end
              end
            end
          end
        }
      else
        Zip::ZipFile.open(zip_file.to_s) do |zip|
          entries = zip.collect
          @paths.each do |path, patterns|
            patterns.map(entries).each do |dest, entry|
              next if entry.directory?
              dest = File.expand_path(dest, target.to_s)
              trace "Extracting #{dest}"
              mkpath File.dirname(dest) rescue nil
              entry.restore_permissions = true
              entry.extract(dest) { true }
            end
          end
        end
      end
      # Let other tasks know we updated the target directory.
      touch target.to_s
    end

    #reads the includes/excludes and apply them to the entry_name
    def included?(entry_name)
      @paths.each do |path, patterns|
        return true if path.nil?
        if entry_name =~ /^#{path}/
          short = entry_name.sub(path, '')
          if patterns.include.any? { |pattern| File.fnmatch(pattern, entry_name) } &&
            !patterns.exclude.any? { |pattern| File.fnmatch(pattern, entry_name) }
            # trace "tar_entry.full_name " + entry_name + " is included"
            return true
          end
        end
      end
      # trace "tar_entry.full_name " + entry_name + " is excluded"
      return false
    end


    # :call-seq:
    #   include(*files) => self
    #   include(*files, :path=>name) => self
    #
    # Include all files that match the patterns and returns self.
    #
    # Use include if you only want to unzip some of the files, by specifying
    # them instead of using exclusion. You can use #include in combination
    # with #exclude.
    def include(*files)
      if Hash === files.last
        from_path(files.pop[:path]).include *files
      else
        from_path(nil).include *files
      end
      self
    end
    alias :add :include

    # :call-seq:
    #   exclude(*files) => self
    #
    # Exclude all files that match the patterns and return self.
    #
    # Use exclude to unzip all files except those that match the pattern.
    # You can use #exclude in combination with #include.
    def exclude(*files)
      if Hash === files.last
        from_path(files.pop[:path]).exclude *files
      else
        from_path(nil).exclude *files
      end
      self
    end

    # :call-seq:
    #   from_path(name) => Path
    #
    # Allows you to unzip from a path. Returns an object you can use to
    # specify which files to include/exclude relative to that path.
    # Expands the file relative to that path.
    #
    # For example:
    #   unzip(Dir.pwd=>'test.jar').from_path('etc').include('LICENSE')
    # will unzip etc/LICENSE into ./LICENSE.
    #
    # This is different from:
    #  unzip(Dir.pwd=>'test.jar').include('etc/LICENSE')
    # which unzips etc/LICENSE into ./etc/LICENSE.
    def from_path(name)
      @paths[name] ||= FromPath.new(self, name)
    end
    alias :path :from_path

    # :call-seq:
    #   root => Unzip
    #
    # Returns the root path, essentially the Unzip object itself. In case you are wondering
    # down paths and want to go back.
    def root
      self
    end

    # Returns the path to the target directory.
    def to_s
      target.to_s
    end

    class FromPath #:nodoc:

      def initialize(unzip, path)
        @unzip = unzip
        if path
          @path = path[-1] == ?/ ? path : path + '/'
        else
          @path = ''
        end
      end

      # See UnzipTask#include
      def include(*files) #:doc:
        @include ||= []
        @include |= files
        self
      end

      # See UnzipTask#exclude
      def exclude(*files) #:doc:
        @exclude ||= []
        @exclude |= files
        self
      end

      def map(entries)
        includes = @include || ['*']
        excludes = @exclude || []
        entries.inject({}) do |map, entry|
          if entry.name =~ /^#{@path}/
            short = entry.name.sub(@path, '')
            if includes.any? { |pat| File.fnmatch(pat, short) } &&
               !excludes.any? { |pat| File.fnmatch(pat, short) }
              map[short] = entry
            end
          end
          map
        end
      end

      # Documented in Unzip.
      def root
        @unzip
      end

      # The target directory to extract to.
      def target
        @unzip.target
      end

    end

  end

  # :call-seq:
  #    unzip(to_dir=>zip_file) => Zip
  #
  # Creates a task that will unzip a file into the target directory. The task name
  # is the target directory, the prerequisite is the file to unzip.
  #
  # This method creates a file task to expand the zip file. It returns an Unzip object
  # that specifies how the file will be extracted. You can include or exclude specific
  # files from within the zip, and map to different paths.
  #
  # The Unzip object's to_s method return the path to the target directory, so you can
  # use it as a prerequisite. By keeping the Unzip object separate from the file task,
  # you overlay additional work on top of the file task.
  #
  # For example:
  #   unzip('all'=>'test.zip')
  #   unzip('src'=>'test.zip').include('README', 'LICENSE')
  #   unzip('libs'=>'test.zip').from_path('libs')
  def unzip(args)
    target, arg_names, zip_file = Buildr.application.resolve_args([args])
    task = file(File.expand_path(target.to_s)=>zip_file)
    Unzip.new(task=>zip_file).tap do |setup|
      task.enhance { setup.extract }
    end
  end

end
