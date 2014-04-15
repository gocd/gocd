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

  # A filter knows how to copy files from one directory to another, applying mappings to the
  # contents of these files.
  #
  # You can specify the mapping using a Hash, and it will map ${key} fields found in each source
  # file into the appropriate value in the target file. For example:
  #
  #   filter.using 'version'=>'1.2', 'build'=>Time.now
  #
  # will replace all occurrences of <tt>${version}</tt> with <tt>1.2</tt>, and <tt>${build}</tt>
  # with the current date/time.
  #
  # You can also specify the mapping by passing a proc or a method, that will be called for
  # each source file, with the file name and content, returning the modified content.
  #
  # Without any mapping, the filter simply copies files from the source directory into the target
  # directory.
  #
  # A filter has one target directory, but you can specify any number of source directories,
  # either when creating the filter or calling #from. Include/exclude patterns are specified
  # relative to the source directories, so:
  #   filter.include '*.png'
  # will only include PNG files from any of the source directories.
  # In the same way, you can use regular expressions, so:
  #   filter.include /picture_.*\.png/
  # will only include PNG files starting with picture_ from any of the sources directories.
  #
  # See Buildr#filter.
  class Filter

    def initialize #:nodoc:
      clear
    end

    # Returns the list of source directories (each being a file task).
    attr_reader :sources

    # :call-seq:
    #   clear => self
    #
    # Clear filter sources and include/exclude patterns
    def clear
      @include = []
      @exclude = []
      @sources = FileList[]
      @mapper = Mapper.new
      self
    end

    # :call-seq:
    #   from(*sources) => self
    #
    # Adds additional directories from which to copy resources.
    #
    # For example:
    #   filter.from('src').into('target').using('build'=>Time.now)
    def from(*sources)
      @sources |= sources.flatten.map { |dir| file(File.expand_path(dir.to_s)) }
      self
    end

    # The target directory as a file task.
    def target
      return nil unless @target_dir
      unless @target
        @target = file(File.expand_path(@target_dir)) { |task| run if @target == task }
        @target.enhance @include.select {|f| f.is_a?(Rake::FileTask)}
        @target.enhance @exclude.select {|f| f.is_a?(Rake::FileTask)}
        @target.enhance copy_map.values
      end
      @target
    end

    # :call-seq:
    #   into(dir) => self
    #
    # Sets the target directory into which files are copied and returns self.
    #
    # For example:
    #   filter.from('src').into('target').using('build'=>Time.now)
    def into(dir)
      @target_dir = dir.to_s
      @target = nil
      self
    end

    # :call-seq:
    #   include(*files) => self
    #
    # Specifies files to include and returns self. See FileList#include.
    #
    # By default all files are included. You can use this method to only include specific
    # files from the source directory.
    def include(*files)
      @include += files.flatten
      self
    end
    alias :add :include

    # :call-seq:
    #   exclude(*files) => self
    #
    # Specifies files to exclude and returns self. See FileList#exclude.
    def exclude(*files)
      @exclude += files.flatten
      self
    end

    # The mapping. See #using.
    def mapping #:nodoc:
      @mapper.config
    end

    # The mapper to use. See #using.
    def mapper #:nodoc:
      @mapper.mapper_type
    end

    # :call-seq:
    #   using(mapping) => self
    #   using { |file_name, contents| ... } => self
    #
    # Specifies the mapping to use and returns self.
    #
    # The most typical mapping uses a Hash, and the default mapping uses the Maven style, so
    # <code>${key}</code> are mapped to the values. You can change that by passing a different
    # format as the first argument. Currently supports:
    # * :ant -- Map <code>@key@</code>.
    # * :maven -- Map <code>${key}</code> (default).
    # * :ruby -- Map <code>#{key}</code>.
    # * :erb -- Map <code><%= key %></code>.
    # * Regexp -- Maps the matched data (e.g. <code>/=(.*?)=/</code>
    #
    # For example:
    #   filter.using 'version'=>'1.2'
    # Is the same as:
    #   filter.using :maven, 'version'=>'1.2'
    #
    # You can also pass a proc or method. It will be called with the file name and content,
    # to return the mapped content.
    #
    # Without any mapping, all files are copied as is.
    #
    # To register new mapping type see the Mapper class.
    def using(*args, &block)
      @mapper.using(*args, &block)
      self
    end

    # :call-seq:
    #    run => boolean
    #
    # Runs the filter.
    def run
      copy_map = copy_map()

      mkpath target.to_s
      return false if copy_map.empty?

      copy_map.each do |path, source|
        dest = File.expand_path(path, target.to_s)
        if File.directory?(source)
          mkpath dest
        else
          mkpath File.dirname(dest)
          if @mapper.mapper_type
            mapped = @mapper.transform(File.open(source, 'rb') { |file| file.read }, path)
            File.open(dest, 'wb') { |file| file.write mapped }
          else # no mapping
            cp source, dest
          end
        end
        File.chmod(File.stat(source).mode | 0200, dest)
      end
      touch target.to_s
      true
    end

    # Returns the target directory.
    def to_s
      target.to_s
    end

  protected
  
    # :call-seq:
    #   pattern_match(file, pattern) => boolean
    # 
    # This method returns true if the file name matches the pattern.
    # The pattern may be a String, a Regexp or a Proc.
    #
    def pattern_match(file, pattern)
      case
      when pattern.is_a?(Regexp)
        return file.match(pattern)
      when pattern.is_a?(String)
        return File.fnmatch(pattern, file)
      when pattern.is_a?(Proc)
        return pattern.call(file)
      when pattern.is_a?(Rake::FileTask)
        return pattern.to_s.match(file)
      else
        raise "Cannot interpret pattern #{pattern}"
      end
    end
    
  private
    def copy_map
      sources.each { |source| raise "Source directory #{source} doesn't exist" unless File.exist?(source.to_s) }
      raise 'No target directory specified, where am I going to copy the files to?' if target.nil?

      sources.flatten.map(&:to_s).inject({}) do |map, source|
        files = Util.recursive_with_dot_files(source).
          map { |file| Util.relative_path(file, source) }.
          select { |file| @include.empty? || @include.any? { |pattern| pattern_match(file, pattern) } }.
          reject { |file| @exclude.any? { |pattern| pattern_match(file, pattern) } }
        files.each do |file|
          src, dest = File.expand_path(file, source), File.expand_path(file, target.to_s)
          map[file] = src if !File.exist?(dest) || File.stat(src).mtime >= File.stat(dest).mtime
        end
        map
      end
    end

    # This class implements content replacement logic for Filter.
    #
    # To register a new template engine @:foo@, extend this class with a method like:
    #
    #   def foo_transform(content, path = nil)
    #      # if this method yields a key, the value comes from the mapping hash
    #      content.gsub(/world/) { |str| yield :bar }
    #   end
    #
    # Then you can use :foo mapping type on a Filter
    #
    #   filter.using :foo, :bar => :baz
    #
    # Or all by your own, simply
    #
    #   Mapper.new(:foo, :bar => :baz).transform("Hello world") # => "Hello baz"
    #
    # You can handle configuration arguments by providing a @*_config@ method like:
    #
    #   # The return value of this method is available with the :config accessor.
    #   def moo_config(*args, &block)
    #      raise ArgumentError, "Expected moo block" unless block_given?
    #      { :moos => args, :callback => block }
    #   end
    #
    #   def moo_transform(content, path = nil)
    #      content.gsub(/moo+/i) do |str|
    #        moos = yield :moos # same than config[:moos]
    #        moo = moos[str.size - 3] || str
    #        config[:callback].call(moo)
    #      end
    #   end
    #
    # Usage for the @:moo@ mapper would be something like:
    #
    #   mapper = Mapper.new(:moo, 'ooone', 'twoo') do |str|
    #     i = nil; str.capitalize.gsub(/\w/) { |s| s.send( (i = !i) ? 'upcase' : 'downcase' ) }
    #   end
    #   mapper.transform('Moo cow, mooo cows singing mooooo') # => 'OoOnE cow, TwOo cows singing MoOoOo'
    class Mapper

      attr_reader :mapper_type, :config

      def initialize(*args, &block) #:nodoc:
        using(*args, &block)
      end

      def using(*args, &block)
        case args.first
        when Hash # Maven hash mapping
          using :maven, *args
        when Binding # Erb binding
          using :erb, *args
        when Symbol # Mapping from a method
          raise ArgumentError, "Unknown mapping type: #{args.first}" unless respond_to?("#{args.first}_transform", true)
          configure(*args, &block)
        when Regexp # Mapping using a regular expression
          raise ArgumentError, 'Expected regular expression followed by mapping hash' unless args.size == 2 && Hash === args[1]
          @mapper_type, @config = *args
        else
          unless args.empty? && block.nil?
            raise ArgumentError, 'Expected proc, method or a block' if args.size > 1 || (args.first && block)
            @mapper_type = :callback
            config = args.first || block
            raise ArgumentError, 'Expected proc, method or callable' unless config.respond_to?(:call)
            @config = config
          end
        end
        self
      end

      def transform(content, path = nil)
        type = Regexp === mapper_type ? :regexp : mapper_type
        raise ArgumentError, "Invalid mapper type: #{type.inspect}" unless respond_to?("#{type}_transform", true)
        self.__send__("#{type}_transform", content, path) { |key| config[key] || config[key.to_s.to_sym] }
      end

     private
      def configure(mapper_type, *args, &block)
        configurer = method("#{mapper_type}_config") rescue nil
        if configurer
          @config = configurer.call(*args, &block)
        else
          raise ArgumentError, "Missing hash argument after :#{mapper_type}" unless args.size == 1 && Hash === args[0]
          @config = {} unless Hash === @config
          args.first.each_pair { |k, v| @config[k] = v.to_s }
        end
        @mapper_type = mapper_type
      end

      def maven_transform(content, path = nil)
        content.gsub(/\$\{.*?\}/) { |str| yield(str[2..-2]) || str }
      end

      def ant_transform(content, path = nil)
        content.gsub(/@.*?@/) { |str| yield(str[1..-2]) || str }
      end

      def ruby_transform(content, path = nil)
        content.gsub(/#\{.*?\}/) { |str| yield(str[2..-2]) || str }
      end

      def regexp_transform(content, path = nil)
        content.gsub(mapper_type) { |str| yield(str.scan(mapper_type).join) || str }
      end

      def callback_transform(content, path = nil)
        config.call(path, content)
      end

      def erb_transform(content, path = nil)
        case config
        when Binding
          bnd = config
        when Hash
          bnd = OpenStruct.new
          table = config.inject({}) { |h, e| h[e.first.to_sym] = e.last; h }
          bnd.instance_variable_set(:@table, table)
          bnd = bnd.instance_eval { binding }
        else
          bnd = config.instance_eval { binding }
        end
        ERB.new(content).result(bnd)
      end

      def erb_config(*args, &block)
        if block_given?
          raise ArgumentError, "Expected block or single argument, but both given." unless args.empty?
          block
        elsif args.size > 1
          raise ArgumentError, "Expected block or single argument."
        else
          args.first
        end
      end

    end # class Mapper

  end

  # :call-seq:
  #   filter(*source) => Filter
  #
  # Creates a filter that will copy files from the source directory(ies) into the target directory.
  # You can extend the filter to modify files by mapping <tt>${key}</tt> into values in each
  # of the copied files, and by including or excluding specific files.
  #
  # A filter is not a task, you must call the Filter#run method to execute it.
  #
  # For example, to copy all files from one directory to another:
  #   filter('src/files').into('target/classes').run
  # To include only the text files, and replace each instance of <tt>${build}</tt> with the current
  # date/time:
  #   filter('src/files').into('target/classes').include('*.txt').using('build'=>Time.now).run
  def filter(*sources)
    Filter.new.from(*sources)
  end

end
