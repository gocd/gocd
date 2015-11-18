module Bundler
  class Source
    class Path < Source
      autoload :Installer, 'bundler/source/path/installer'

      attr_reader   :path, :options
      attr_writer   :name
      attr_accessor :version

      DEFAULT_GLOB = "{,*,*/*}.gemspec"

      def initialize(options)
        @options = options
        @glob = options["glob"] || DEFAULT_GLOB

        @allow_cached = false
        @allow_remote = false

        if options["path"]
          @path = Pathname.new(options["path"])
          @path = expand(@path) unless @path.relative?
        end

        @name    = options["name"]
        @version = options["version"]

        # Stores the original path. If at any point we move to the
        # cached directory, we still have the original path to copy from.
        @original_path = @path
      end

      def remote!
        @allow_remote = true
      end

      def cached!
        @allow_cached = true
      end

      def self.from_lock(options)
        new(options.merge("path" => options.delete("remote")))
      end

      def to_lock
        out = "PATH\n"
        out << "  remote: #{relative_path}\n"
        out << "  glob: #{@glob}\n" unless @glob == DEFAULT_GLOB
        out << "  specs:\n"
      end

      def to_s
        "source at #{@path}"
      end

      def hash
        [self.class, expanded_path, version].hash
      end

      def eql?(o)
        o.instance_of?(Path) &&
        expanded_path == expand(o.path) &&
        version == o.version
      end

      alias == eql?

      def name
        File.basename(expanded_path.to_s)
      end

      def install(spec, force = false)
        Bundler.ui.info "Using #{version_message(spec)} from #{to_s}"
        generate_bin(spec, :disable_extensions)
        nil # no post-install message
      end

      def cache(spec, custom_path = nil)
        app_cache_path = app_cache_path(custom_path)
        return unless Bundler.settings[:cache_all]
        return if expand(@original_path).to_s.index(Bundler.root.to_s) == 0

        unless @original_path.exist?
          raise GemNotFound, "Can't cache gem #{version_message(spec)} because #{to_s} is missing!"
        end

        FileUtils.rm_rf(app_cache_path)
        FileUtils.cp_r("#{@original_path}/.", app_cache_path)
        FileUtils.touch(app_cache_path.join(".bundlecache"))
      end

      def local_specs(*)
        @local_specs ||= load_spec_files
      end

      def specs
        if has_app_cache?
          @path = app_cache_path
          @expanded_path = nil # Invalidate
        end
        local_specs
      end

      def app_cache_dirname
        name
      end

    private

      def expanded_path
        @expanded_path ||= expand(path)
      end

      def expand(somepath)
        somepath.expand_path(Bundler.root)
      rescue ArgumentError => e
        Bundler.ui.debug(e)
        raise PathError, "There was an error while trying to use the path " \
          "`#{somepath}`.\nThe error message was: #{e.message}."
      end

      def app_cache_path(custom_path = nil)
        @app_cache_path ||= Bundler.app_cache(custom_path).join(app_cache_dirname)
      end

      def has_app_cache?
        SharedHelpers.in_bundle? && app_cache_path.exist?
      end

      def load_spec_files
        index = Index.new

        if File.directory?(expanded_path)
          # We sort depth-first since `<<` will override the earlier-found specs
          Dir["#{expanded_path}/#{@glob}"].sort_by { |p| -p.split(File::SEPARATOR).size }.each do |file|
            if spec = Bundler.load_gemspec(file, :validate)
              spec.loaded_from = file.to_s
              spec.source = self
              index << spec
            end
          end

          if index.empty? && @name && @version
            index << Gem::Specification.new do |s|
              s.name     = @name
              s.source   = self
              s.version  = Gem::Version.new(@version)
              s.platform = Gem::Platform::RUBY
              s.summary  = "Fake gemspec for #{@name}"
              s.relative_loaded_from = "#{@name}.gemspec"
              s.authors  = ["no one"]
              if expanded_path.join("bin").exist?
                executables = expanded_path.join("bin").children
                executables.reject!{|p| File.directory?(p) }
                s.executables = executables.map{|c| c.basename.to_s }
              end
            end
          end
        elsif File.exist?(expanded_path)
          raise PathError, "The path `#{expanded_path}` is not a directory."
        else
          raise PathError, "The path `#{expanded_path}` does not exist."
        end

        index
      end

      def relative_path
        if path.to_s.match(%r{^#{Regexp.escape Bundler.root.to_s}})
          return path.relative_path_from(Bundler.root)
        end
        path
      end

      def generate_bin(spec, disable_extensions = false)
        gem_dir = Pathname.new(spec.full_gem_path)

        # Some gem authors put absolute paths in their gemspec
        # and we have to save them from themselves
        spec.files = spec.files.map do |p|
          next if File.directory?(p)
          begin
            Pathname.new(p).relative_path_from(gem_dir).to_s
          rescue ArgumentError
            p
          end
        end.compact

        SharedHelpers.chdir(gem_dir) do
          installer = Path::Installer.new(spec, :env_shebang => false)
          run_hooks(:pre_install, installer)
          installer.build_extensions unless disable_extensions
          run_hooks(:post_build, installer)
          installer.generate_bin
          run_hooks(:post_install, installer)
        end
      rescue Gem::InvalidSpecificationException => e
        Bundler.ui.warn "\n#{spec.name} at #{spec.full_gem_path} did not have a valid gemspec.\n" \
                        "This prevents bundler from installing bins or native extensions, but " \
                        "that may not affect its functionality."

        if !spec.extensions.empty? && !spec.email.empty?
          Bundler.ui.warn "If you need to use this package without installing it from a gem " \
                          "repository, please contact #{spec.email} and ask them " \
                          "to modify their .gemspec so it can work with `gem build`."
        end

        Bundler.ui.warn "The validation message from Rubygems was:\n  #{e.message}"
      end

      def run_hooks(type, installer)
        hooks_meth = "#{type}_hooks"
        return unless Gem.respond_to?(hooks_meth)
        Gem.send(hooks_meth).each do |hook|
          result = hook.call(installer)
          if result == false
            location = " at #{$1}" if hook.inspect =~ /@(.*:\d+)/
            message = "#{type} hook#{location} failed for #{installer.spec.full_name}"
            raise InstallHookError, message
          end
        end
      end

    end
  end
end
