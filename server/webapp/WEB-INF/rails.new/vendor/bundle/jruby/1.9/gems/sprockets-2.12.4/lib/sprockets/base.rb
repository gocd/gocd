require 'sprockets/asset_attributes'
require 'sprockets/bundled_asset'
require 'sprockets/caching'
require 'sprockets/errors'
require 'sprockets/processed_asset'
require 'sprockets/server'
require 'sprockets/static_asset'
require 'multi_json'
require 'pathname'

module Sprockets
  # `Base` class for `Environment` and `Index`.
  class Base
    include Caching, Paths, Mime, Processing, Compressing, Engines, Server

    # Returns a `Digest` implementation class.
    #
    # Defaults to `Digest::MD5`.
    attr_reader :digest_class

    # Assign a `Digest` implementation class. This may be any Ruby
    # `Digest::` implementation such as `Digest::MD5` or
    # `Digest::SHA1`.
    #
    #     environment.digest_class = Digest::SHA1
    #
    def digest_class=(klass)
      expire_index!
      @digest_class = klass
    end

    # The `Environment#version` is a custom value used for manually
    # expiring all asset caches.
    #
    # Sprockets is able to track most file and directory changes and
    # will take care of expiring the cache for you. However, its
    # impossible to know when any custom helpers change that you mix
    # into the `Context`.
    #
    # It would be wise to increment this value anytime you make a
    # configuration change to the `Environment` object.
    attr_reader :version

    # Assign an environment version.
    #
    #     environment.version = '2.0'
    #
    def version=(version)
      expire_index!
      @version = version
    end

    # Returns a `Digest` instance for the `Environment`.
    #
    # This value serves two purposes. If two `Environment`s have the
    # same digest value they can be treated as equal. This is more
    # useful for comparing environment states between processes rather
    # than in the same. Two equal `Environment`s can share the same
    # cached assets.
    #
    # The value also provides a seed digest for all `Asset`
    # digests. Any change in the environment digest will affect all of
    # its assets.
    def digest
      # Compute the initial digest using the implementation class. The
      # Sprockets release version and custom environment version are
      # mixed in. So any new releases will affect all your assets.
      @digest ||= digest_class.new.update(VERSION).update(version.to_s)

      # Returned a dupped copy so the caller can safely mutate it with `.update`
      @digest.dup
    end

    # Get and set `Logger` instance.
    attr_accessor :logger

    # Get `Context` class.
    #
    # This class maybe mutated and mixed in with custom helpers.
    #
    #     environment.context_class.instance_eval do
    #       include MyHelpers
    #       def asset_url; end
    #     end
    #
    attr_reader :context_class

    # Get persistent cache store
    attr_reader :cache

    # Set persistent cache store
    #
    # The cache store must implement a pair of getters and
    # setters. Either `get(key)`/`set(key, value)`,
    # `[key]`/`[key]=value`, `read(key)`/`write(key, value)`.
    def cache=(cache)
      expire_index!
      @cache = cache
    end

    def prepend_path(path)
      # Overrides the global behavior to expire the index
      expire_index!
      super
    end

    def append_path(path)
      # Overrides the global behavior to expire the index
      expire_index!
      super
    end

    def clear_paths
      # Overrides the global behavior to expire the index
      expire_index!
      super
    end

    # Finds the expanded real path for a given logical path by
    # searching the environment's paths.
    #
    #     resolve("application.js")
    #     # => "/path/to/app/javascripts/application.js.coffee"
    #
    # A `FileNotFound` exception is raised if the file does not exist.
    def resolve(logical_path, options = {})
      # If a block is given, preform an iterable search
      if block_given?
        args = attributes_for(logical_path).search_paths + [options]
        @trail.find(*args) do |path|
          pathname = Pathname.new(path)
          if %w( .bower.json bower.json component.json ).include?(pathname.basename.to_s)
            bower = json_decode(pathname.read)
            case bower['main']
            when String
              yield pathname.dirname.join(bower['main'])
            when Array
              extname = File.extname(logical_path)
              bower['main'].each do |fn|
                if extname == "" || extname == File.extname(fn)
                  yield pathname.dirname.join(fn)
                end
              end
            end
          else
            yield pathname
          end
        end
      else
        resolve(logical_path, options) do |pathname|
          return pathname
        end
        raise FileNotFound, "couldn't find file '#{logical_path}'"
      end
    end

    # Register a new mime type.
    def register_mime_type(mime_type, ext)
      # Overrides the global behavior to expire the index
      expire_index!
      @trail.append_extension(ext)
      super
    end

    # Registers a new Engine `klass` for `ext`.
    def register_engine(ext, klass)
      # Overrides the global behavior to expire the index
      expire_index!
      add_engine_to_trail(ext, klass)
      super
    end

    def register_preprocessor(mime_type, klass, &block)
      # Overrides the global behavior to expire the index
      expire_index!
      super
    end

    def unregister_preprocessor(mime_type, klass)
      # Overrides the global behavior to expire the index
      expire_index!
      super
    end

    def register_postprocessor(mime_type, klass, &block)
      # Overrides the global behavior to expire the index
      expire_index!
      super
    end

    def unregister_postprocessor(mime_type, klass)
      # Overrides the global behavior to expire the index
      expire_index!
      super
    end

    def register_bundle_processor(mime_type, klass, &block)
      # Overrides the global behavior to expire the index
      expire_index!
      super
    end

    def unregister_bundle_processor(mime_type, klass)
      # Overrides the global behavior to expire the index
      expire_index!
      super
    end

    # Return an `Index`. Must be implemented by the subclass.
    def index
      raise NotImplementedError
    end

    if defined? Encoding.default_external
      # Define `default_external_encoding` accessor on 1.9.
      # Defaults to UTF-8.
      attr_accessor :default_external_encoding
    end

    # Works like `Dir.entries`.
    #
    # Subclasses may cache this method.
    def entries(pathname)
      @trail.entries(pathname)
    end

    # Works like `File.stat`.
    #
    # Subclasses may cache this method.
    def stat(path)
      @trail.stat(path)
    end

    # Read and compute digest of filename.
    #
    # Subclasses may cache this method.
    def file_digest(path)
      if stat = self.stat(path)
        # If its a file, digest the contents
        if stat.file?
          digest.file(path.to_s)

        # If its a directive, digest the list of filenames
        elsif stat.directory?
          contents = self.entries(path).join(',')
          digest.update(contents)
        end
      end
    end

    # Internal. Return a `AssetAttributes` for `path`.
    def attributes_for(path)
      AssetAttributes.new(self, path)
    end

    # Internal. Return content type of `path`.
    def content_type_of(path)
      attributes_for(path).content_type
    end

    # Find asset by logical path or expanded path.
    def find_asset(path, options = {})
      logical_path = path
      pathname     = Pathname.new(path).cleanpath

      if pathname.absolute?
        return unless stat(pathname)
        logical_path = attributes_for(pathname).logical_path
      else
        begin
          pathname = resolve(logical_path)

          # If logical path is missing a mime type extension, append
          # the absolute path extname so it has one.
          #
          # Ensures some consistency between finding "foo/bar" vs
          # "foo/bar.js".
          if File.extname(logical_path) == ""
            expanded_logical_path = attributes_for(pathname).logical_path
            logical_path += File.extname(expanded_logical_path)
          end
        rescue FileNotFound
          return nil
        end
      end

      build_asset(logical_path, pathname, options)
    end

    # Preferred `find_asset` shorthand.
    #
    #     environment['application.js']
    #
    def [](*args)
      find_asset(*args)
    end

    def each_entry(root, &block)
      return to_enum(__method__, root) unless block_given?
      root = Pathname.new(root) unless root.is_a?(Pathname)

      paths = []
      entries(root).sort.each do |filename|
        path = root.join(filename)
        paths << path

        if stat(path).directory?
          each_entry(path) do |subpath|
            paths << subpath
          end
        end
      end

      paths.sort_by(&:to_s).each(&block)

      nil
    end

    def each_file
      return to_enum(__method__) unless block_given?
      paths.each do |root|
        each_entry(root) do |path|
          if !stat(path).directory?
            yield path
          end
        end
      end
      nil
    end

    def each_logical_path(*args, &block)
      return to_enum(__method__, *args) unless block_given?
      filters = args.flatten
      files = {}
      each_file do |filename|
        if logical_path = logical_path_for_filename(filename, filters)
          unless files[logical_path]
            if block.arity == 2
              yield logical_path, filename.to_s
            else
              yield logical_path
            end
          end

          files[logical_path] = true
        end
      end
      nil
    end

    # Pretty inspect
    def inspect
      "#<#{self.class}:0x#{object_id.to_s(16)} " +
        "root=#{root.to_s.inspect}, " +
        "paths=#{paths.inspect}, " +
        "digest=#{digest.to_s.inspect}" +
        ">"
    end

    protected
      # Clear index after mutating state. Must be implemented by the subclass.
      def expire_index!
        raise NotImplementedError
      end

      def build_asset(logical_path, pathname, options)
        pathname = Pathname.new(pathname)

        # If there are any processors to run on the pathname, use
        # `BundledAsset`. Otherwise use `StaticAsset` and treat is as binary.
        if attributes_for(pathname).processors.any?
          if options[:bundle] == false
            circular_call_protection(pathname.to_s) do
              ProcessedAsset.new(index, logical_path, pathname)
            end
          else
            BundledAsset.new(index, logical_path, pathname)
          end
        else
          StaticAsset.new(index, logical_path, pathname)
        end
      end

      def cache_key_for(path, options)
        "#{path}:#{options[:bundle] ? '1' : '0'}"
      end

      def circular_call_protection(path)
        reset = Thread.current[:sprockets_circular_calls].nil?
        calls = Thread.current[:sprockets_circular_calls] ||= Set.new
        if calls.include?(path)
          raise CircularDependencyError, "#{path} has already been required"
        end
        calls << path
        yield
      ensure
        Thread.current[:sprockets_circular_calls] = nil if reset
      end

      def logical_path_for_filename(filename, filters)
        logical_path = attributes_for(filename).logical_path.to_s

        if matches_filter(filters, logical_path, filename)
          return logical_path
        end

        # If filename is an index file, retest with alias
        if File.basename(logical_path)[/[^\.]+/, 0] == 'index'
          path = logical_path.sub(/\/index\./, '.')
          if matches_filter(filters, path, filename)
            return path
          end
        end

        nil
      end

      def matches_filter(filters, logical_path, filename)
        return true if filters.empty?

        filters.any? do |filter|
          if filter.is_a?(Regexp)
            filter.match(logical_path)
          elsif filter.respond_to?(:call)
            if filter.arity == 1
              filter.call(logical_path)
            else
              filter.call(logical_path, filename.to_s)
            end
          else
            File.fnmatch(filter.to_s, logical_path)
          end
        end
      end

      # Feature detect newer MultiJson API
      if MultiJson.respond_to?(:dump)
        def json_decode(obj)
          MultiJson.load(obj)
        end
      else
        def json_decode(obj)
          MultiJson.decode(obj)
        end
      end
  end
end
