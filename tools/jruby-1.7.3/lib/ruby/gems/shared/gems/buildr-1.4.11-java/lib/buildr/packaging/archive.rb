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

  # Base class for ZipTask, TarTask and other archives.
  class ArchiveTask < Rake::FileTask

    # Which files go where. All the rules for including, excluding and merging files
    # are handled by this object.
    class Path #:nodoc:

      # Returns the archive from this path.
      attr_reader :root

      def initialize(root, path)
        @root = root
        @path = path.empty? ? path : "#{path}/"
        @includes = FileList[]
        @excludes = []
        # Expand source files added to this path.
        expand_src = proc { @includes.map{ |file| file.to_s }.uniq }
        @sources = [ expand_src ]
        # Add files and directories added to this path.
        @actions = [] << proc do |file_map|
          expand_src.call.each do |path|
            unless excluded?(path)
              if File.directory?(path)
                in_directory path do |file, rel_path|
                  dest = "#{@path}#{rel_path}"
                  unless excluded?(dest)
                    trace "Adding #{dest}"
                    file_map[dest] = file
                  end
                end
              end
              unless File.basename(path) == "."
                trace "Adding #{@path}#{File.basename(path)}"
                file_map["#{@path}#{File.basename(path)}"] = path
              end
            end
          end
        end
      end

      # :call-seq:
      #   include(*files) => self
      #   include(*files, :path=>path) => self
      #   include(file, :as=>name) => self
      #   include(:from=>path) => self
      #   include(*files, :merge=>true) => self
      def include(*args)
        options = args.pop if Hash === args.last
        files = to_artifacts(args)
        raise 'AchiveTask.include() values should not include nil' if files.include? nil

        if options.nil? || options.empty?
          @includes.include *files.flatten
        elsif options[:path]
          sans_path = options.reject { |k,v| k == :path }
          path(options[:path]).include *files + [sans_path]
        elsif options[:as]
          raise 'You can only use the :as option in combination with the :path option' unless options.size == 1
          raise 'You can only use one file with the :as option' unless files.size == 1
          include_as files.first.to_s, options[:as]
        elsif options[:from]
          raise 'You can only use the :from option in combination with the :path option' unless options.size == 1
          raise 'You cannot use the :from option with file names' unless files.empty?
          fail 'AchiveTask.include() :from value should not be nil' if [options[:from]].flatten.include? nil
          [options[:from]].flatten.each { |path| include_as path.to_s, '.' }
        elsif options[:merge]
          raise 'You can only use the :merge option in combination with the :path option' unless options.size == 1
          files.each { |file| merge file }
        else
          raise "Unrecognized option #{options.keys.join(', ')}"
        end
        self
      end
      alias :add :include
      alias :<< :include

      # :call-seq:
      #   exclude(*files) => self
      def exclude(*files)
        files = to_artifacts(files)
        @excludes |= files
        @excludes |= files.reject { |f| f =~ /\*$/ }.map { |f| "#{f}/*" }
        self
      end

      # :call-seq:
      #   merge(*files) => Merge
      #   merge(*files, :path=>name) => Merge
      def merge(*args)
        options = Hash === args.last ? args.pop : {}
        files = to_artifacts(args)
        rake_check_options options, :path
        raise ArgumentError, "Expected at least one file to merge" if files.empty?
        path = options[:path] || @path
        expanders = files.collect do |file|
          @sources << proc { file.to_s }
          expander = ZipExpander.new(file)
          @actions << proc do |file_map|
            file.invoke() if file.is_a?(Rake::Task)
            expander.expand(file_map, path)
          end
          expander
        end
        Merge.new(expanders)
      end

      # Returns a Path relative to this one.
      def path(path)
        return self if path.nil?
        return root.path(path[1..-1]) if path[0] == ?/
        root.path("#{@path}#{path}")
      end

      # Returns all the source files.
      def sources #:nodoc:
        @sources.map{ |source| source.call }.flatten
      end

      def add_files(file_map) #:nodoc:
        @actions.each { |action| action.call(file_map) }
      end

      # :call-seq:
      #   exist => boolean
      #
      # Returns true if this path exists. This only works if the path has any entries in it,
      # so exist on path happens to be the opposite of empty.
      def exist?
        !entries.empty?
      end

      # :call-seq:
      #   empty? => boolean
      #
      # Returns true if this path is empty (has no other entries inside).
      def empty?
        entries.all? { |entry| entry.empty? }
      end

      # :call-seq:
      #   contain(file*) => boolean
      #
      # Returns true if this ZIP file path contains all the specified files. You can use relative
      # file names and glob patterns (using *, **, etc).
      def contain?(*files)
        files.all? { |file| entries.detect { |entry| File.fnmatch(file, entry.to_s) } }
      end

      # :call-seq:
      #   entry(name) => ZipEntry
      #
      # Returns a ZIP file entry. You can use this to check if the entry exists and its contents,
      # for example:
      #   package(:jar).path("META-INF").entry("LICENSE").should contain(/Apache Software License/)
      def entry(name)
        root.entry("#{@path}#{name}")
      end

      def to_s
        @path
      end

    protected

    # Convert objects to artifacts, where applicable
    def to_artifacts(files)
      files.flatten.inject([]) do |set, file|
        case file
        when ArtifactNamespace
          set |= file.artifacts
        when Symbol, Hash
          set |= [artifact(file)]
        when /([^:]+:){2,4}/ # A spec as opposed to a file name.
          set |= [Buildr.artifact(file)]
        when Project
          set |= Buildr.artifacts(file.packages)
        when Rake::Task
          set |= [file]
        when Struct
          set |= Buildr.artifacts(file.values)
        else
          # non-artifacts passed as-is; in particular, String paths are
          # unmodified since Rake FileTasks don't use absolute paths
          set |= [file]
        end
      end
    end

    def include_as(source, as)
        @sources << proc { source }
        @actions << proc do |file_map|
          file = source.to_s
          unless excluded?(file)
            if File.directory?(file)
              in_directory file do |file, rel_path|
                path = rel_path.split('/')[1..-1]
                path.unshift as unless as == '.'
                dest = "#{@path}#{path.join('/')}"
                unless excluded?(dest)
                  trace "Adding #{dest}"
                  file_map[dest] = file
                end
              end
              unless as == "."
                trace "Adding #{@path}#{as}/"
                file_map["#{@path}#{as}/"] = nil # :as is a folder, so the trailing / is required.
              end
            else
              file_map["#{@path}#{as}"] = file
            end

          end
        end
      end

      def in_directory(dir)
        prefix = Regexp.new('^' + Regexp.escape(File.dirname(dir) + File::SEPARATOR))
        Util.recursive_with_dot_files(dir).reject { |file| excluded?(file) }.
          each { |file| yield file, file.sub(prefix, '') }
      end

      def excluded?(file)
        @excludes.any? { |exclude| File.fnmatch(exclude, file) }
      end

      def entries #:nodoc:
        return root.entries unless @path
        @entries ||= root.entries.inject([]) { |selected, entry|
          selected << entry.name.sub(@path, "") if entry.name.index(@path) == 0
          selected
        }
      end

    end


    class Merge
      def initialize(expanders)
        @expanders = expanders
      end

      def include(*files)
        @expanders.each { |expander| expander.include(*files) }
        self
      end
      alias :<< :include

      def exclude(*files)
        @expanders.each { |expander| expander.exclude(*files) }
        self
      end
    end


    # Extend one Zip file into another.
    class ZipExpander #:nodoc:

      def initialize(zip_file)
        @zip_file = zip_file.to_s
        @includes = []
        @excludes = []
      end

      def include(*files)
        @includes |= files
        self
      end
      alias :<< :include

      def exclude(*files)
        @excludes |= files
        self
      end

      def expand(file_map, path)
        @includes = ['*'] if @includes.empty?
        Zip::ZipFile.open(@zip_file) do |source|
          source.entries.reject { |entry| entry.directory? }.each do |entry|
            if @includes.any? { |pattern| File.fnmatch(pattern, entry.name) } &&
               !@excludes.any? { |pattern| File.fnmatch(pattern, entry.name) }
              dest = path =~ /^\/?$/ ? entry.name : Util.relative_path(path + "/" + entry.name)
              trace "Adding #{dest}"
              file_map[dest] = lambda { |output| output.write source.read(entry) }
            end
          end
        end
      end

    end


    def initialize(*args) #:nodoc:
      super
      clean

      # Make sure we're the last enhancements, so other enhancements can add content.
      enhance do
        @file_map = {}
        enhance do
          send 'create' if respond_to?(:create)
          # We're here because the archive file does not exist, or one of the files is newer than the archive contents;
          # we need to make sure the archive doesn't exist (e.g. opening an existing Zip will add instead of create).
          # We also want to protect against partial updates.
          rm name rescue nil
          mkpath File.dirname(name)
          begin
            @paths.each do |name, object|
              @file_map[name] = nil unless name.empty?
              object.add_files(@file_map)
            end
            create_from @file_map
          rescue
            rm name rescue nil
            raise
          end
        end
      end
    end

    # :call-seq:
    #   clean => self
    #
    # Removes all previously added content from this archive.
    # Use this method if you want to remove default content from a package.
    # For example, package(:jar) by default includes compiled classes and resources,
    # using this method, you can create an empty jar and afterwards add the
    # desired content to it.
    #
    #    package(:jar).clean.include path_to('desired/content')
    def clean
      @paths = { '' => Path.new(self, '') }
      @prepares = []
      self
    end

    # :call-seq:
    #   include(*files) => self
    #   include(*files, :path=>path) => self
    #   include(file, :as=>name) => self
    #   include(:from=>path) => self
    #   include(*files, :merge=>true) => self
    #
    # Include files in this archive, or when called on a path, within that path. Returns self.
    #
    # The first form accepts a list of files, directories and glob patterns and adds them to the archive.
    # For example, to include the file foo, directory bar (including all files in there) and all files under baz:
    #   zip(..).include('foo', 'bar', 'baz/*')
    #
    # The second form is similar but adds files/directories under the specified path. For example,
    # to add foo as bar/foo:
    #   zip(..).include('foo', :path=>'bar')
    # The :path option is the same as using the path method:
    #   zip(..).path('bar').include('foo')
    # All other options can be used in combination with the :path option.
    #
    # The third form adds a file or directory under a different name. For example, to add the file foo under the
    # name bar:
    #   zip(..).include('foo', :as=>'bar')
    #
    # The fourth form adds the contents of a directory using the directory as a prerequisite:
    #   zip(..).include(:from=>'foo')
    # Unlike <code>include('foo')</code> it includes the contents of the directory, not the directory itself.
    # Unlike <code>include('foo/*')</code>, it uses the directory timestamp for dependency management.
    #
    # The fifth form includes the contents of another archive by expanding it into this archive. For example:
    #   zip(..).include('foo.zip', :merge=>true).include('bar.zip')
    # You can also use the method #merge.
    def include(*files)
      fail "AchiveTask.include() called with nil values" if files.include? nil
      @paths[''].include *files if files.compact.size > 0
      self
    end
    alias :add :include
    alias :<< :include

    # :call-seq:
    #   exclude(*files) => self
    #
    # Excludes files and returns self. Can be used in combination with include to prevent some files from being included.
    def exclude(*files)
      @paths[''].exclude *files
      self
    end

    # :call-seq:
    #   merge(*files) => Merge
    #   merge(*files, :path=>name) => Merge
    #
    # Merges another archive into this one by including the individual files from the merged archive.
    #
    # Returns an object that supports two methods: include and exclude. You can use these methods to merge
    # only specific files. For example:
    #   zip(..).merge('src.zip').include('module1/*')
    def merge(*files)
      @paths[''].merge *files
    end

    # :call-seq:
    #   path(name) => Path
    #
    # Returns a path object. Use the path object to include files under a path, for example, to include
    # the file 'foo' as 'bar/foo':
    #   zip(..).path('bar').include('foo')
    #
    # Returns a Path object. The Path object implements all the same methods, like include, exclude, merge
    # and so forth. It also implements path and root, so that:
    #   path('foo').path('bar') == path('foo/bar')
    #   path('foo').root == root
    def path(name)
      return @paths[''] if name.nil?
      normalized = name.split('/').inject([]) do |path, part|
        case part
        when '.', nil, ''
          path
        when '..'
          path[0...-1]
        else
          path << part
        end
      end.join('/')
      @paths[normalized] ||= Path.new(self, normalized)
    end

    # :call-seq:
    #   root => ArchiveTask
    #
    # Call this on an archive to return itself, and on a path to return the archive.
    def root
      self
    end

    # :call-seq:
    #   with(options) => self
    #
    # Passes options to the task and returns self. Some tasks support additional options, for example,
    # the WarTask supports options like :manifest, :libs and :classes.
    #
    # For example:
    #   package(:jar).with(:manifest=>'MANIFEST_MF')
    def with(options)
      options.each do |key, value|
        begin
          send "#{key}=", value
        rescue NoMethodError
          raise ArgumentError, "#{self.class.name} does not support the option #{key}"
        end
      end
      self
    end

    def invoke_prerequisites(args, chain) #:nodoc:
      @prepares.each { |prepare| prepare.call(self) }
      @prepares.clear

      file_map = {}
      @paths.each do |name, path|
        path.add_files(file_map)
      end

      # filter out Procs (dynamic content), nils and others
      @prerequisites |= file_map.values.select { |src| src.is_a?(String) || src.is_a?(Rake::Task) }

      super
    end

    def needed? #:nodoc:
      return true unless File.exist?(name)
      # You can do something like:
      #   include('foo', :path=>'foo').exclude('foo/bar', path=>'foo').
      #     include('foo/bar', :path=>'foo/bar')
      # This will play havoc if we handled all the prerequisites together
      # under the task, so instead we handle them individually for each path.
      #
      # We need to check that any file we include is not newer than the
      # contents of the Zip. The file itself but also the directory it's
      # coming from, since some tasks touch the directory, e.g. when the
      # content of target/classes is included into a WAR.
      most_recent = @paths.collect { |name, path| path.sources }.flatten.
        select { |file| File.exist?(file) }.collect { |file| File.stat(file).mtime }.max
      File.stat(name).mtime < (most_recent || Rake::EARLY) || super
    end

    # :call-seq:
    #   empty? => boolean
    #
    # Returns true if this ZIP file is empty (has no other entries inside).
    def empty?
      path("").empty
    end

    # :call-seq:
    #   contain(file*) => boolean
    #
    # Returns true if this ZIP file contains all the specified files. You can use absolute
    # file names and glob patterns (using *, **, etc).
    def contain?(*files)
      path("").contain?(*files)
    end

  protected

    # Adds a prepare block. These blocks are called early on for adding more content to
    # the archive, before invoking prerequsities. Anything you add here will be invoked
    # as a prerequisite and used to determine whether or not to generate this archive.
    # In contrast, enhance blocks are evaluated after it was decided to create this archive.
    def prepare(&block)
      @prepares << block
    end

    def []=(key, value) #:nodoc:
      raise ArgumentError, "This task does not support the option #{key}."
    end

  end


end
