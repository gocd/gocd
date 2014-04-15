require 'rubygems'
# rubygems master requires UI for ConfigFile but doesn't require it
require 'rubygems/user_interaction'
require 'rubygems/config_file'

module Bundler
  class RubygemsIntegration

    def build_args
      Gem::Command.build_args
    end

    def build_args=(args)
      Gem::Command.build_args = args
    end

    def loaded_specs(name)
      Gem.loaded_specs[name]
    end

    def mark_loaded(spec)
      Gem.loaded_specs[spec.name] = spec
    end

    def path(obj)
      obj.to_s
    end

    def platforms
      Gem.platforms
    end

    def configuration
      Gem.configuration
    end

    def ruby_engine
      Gem.ruby_engine
    end

    def read_binary(path)
      Gem.read_binary(path)
    end

    def inflate(obj)
      Gem.inflate(obj)
    end

    def sources=(val)
      # Gem.configuration creates a new Gem::ConfigFile, which by default will read ~/.gemrc
      # If that file exists, its settings (including sources) will overwrite the values we
      # are about to set here. In order to avoid that, we force memoizing the config file now.
      configuration

      Gem.sources = val
    end

    def sources
      Gem.sources
    end

    def gem_dir
      Gem.dir
    end

    def gem_bindir
      Gem.bindir
    end

    def user_home
      Gem.user_home
    end

    def gem_path
      Gem.path
    end

    def marshal_spec_dir
      Gem::MARSHAL_SPEC_DIR
    end

    def clear_paths
      Gem.clear_paths
    end

    def bin_path(gem, bin, ver)
      Gem.bin_path(gem, bin, ver)
    end

    def refresh
      Gem.refresh
    end

    def preserve_paths
      # this is a no-op outside of Rubygems 1.8
      yield
    end

    def ui=(obj)
      Gem::DefaultUserInteraction.ui = obj
    end

    def fetch_specs(all, pre, &blk)
      specs = Gem::SpecFetcher.new.list(all, pre)
      specs.each { yield } if block_given?
      specs
    end

    def fetch_prerelease_specs
      fetch_specs(false, true)
    rescue Gem::RemoteFetcher::FetchError
      [] # if we can't download them, there aren't any
    end

    def fetch_all_remote_specs
      # Fetch all specs, minus prerelease specs
      spec_list = fetch_specs(true, false)
      # Then fetch the prerelease specs
      fetch_prerelease_specs.each {|k, v| spec_list[k] += v }

      return spec_list
    end

    def with_build_args(args)
      old_args = self.build_args
      begin
        self.build_args = args
        yield
      ensure
        self.build_args = old_args
      end
    end

    def gem_from_path(path, policy = nil)
      require 'rubygems/format'
      Gem::Format.from_file_by_path(path, policy)
    end

    def spec_from_gem(path, policy = nil)
      require 'rubygems/security'
      gem_from_path(path, Gem::Security::Policies[policy]).spec
    rescue Gem::Package::FormatError
      raise GemspecError, "Could not read gem at #{path}. It may be corrupted."
    rescue Exception, Gem::Exception, Gem::Security::Exception => e
      if e.is_a?(Gem::Security::Exception) ||
          e.message =~ /unknown trust policy|unsigned gem/i ||
          e.message =~ /couldn't verify (meta)?data signature/i
        raise SecurityError,
          "The gem #{File.basename(path, '.gem')} can't be installed because " \
          "the security policy didn't allow it, with the message: #{e.message}"
      else
        raise e
      end
    end

    def build(spec)
      require 'rubygems/builder'
      Gem::Builder.new(spec).build
    end

    def build_gem(gem_dir, spec)
      Dir.chdir(gem_dir) { build(spec) }
    end

    def download_gem(spec, uri, path)
      Gem::RemoteFetcher.fetcher.download(spec, uri, path)
    end

    def security_policies
      @security_policies ||= begin
        require 'rubygems/security'
        Gem::Security::Policies
      rescue LoadError
        {}
      end
    end

    def reverse_rubygems_kernel_mixin
      # Disable rubygems' gem activation system
      ::Kernel.class_eval do
        if private_method_defined?(:gem_original_require)
          alias rubygems_require require
          alias require gem_original_require
        end

        undef gem
      end
    end

    def replace_gem(specs)
      executables = specs.map { |s| s.executables }.flatten

      ::Kernel.send(:define_method, :gem) do |dep, *reqs|
        if executables.include? File.basename(caller.first.split(':').first)
          return
        end
        reqs.pop if reqs.last.is_a?(Hash)

        unless dep.respond_to?(:name) && dep.respond_to?(:requirement)
          dep = Gem::Dependency.new(dep, reqs)
        end

        spec = specs.find  { |s| s.name == dep.name }

        if spec.nil?

          e = Gem::LoadError.new "#{dep.name} is not part of the bundle. Add it to Gemfile."
          e.name = dep.name
          if e.respond_to?(:requirement=)
            e.requirement = dep.requirement
          else
            e.version_requirement = dep.requirement
          end
          raise e
        elsif dep !~ spec
          e = Gem::LoadError.new "can't activate #{dep}, already activated #{spec.full_name}. " \
                                 "Make sure all dependencies are added to Gemfile."
          e.name = dep.name
          if e.respond_to?(:requirement=)
            e.requirement = dep.requirement
          else
            e.version_requirement = dep.requirement
          end
          raise e
        end

        true
      end
    end

    def stub_source_index137(specs)
      # Rubygems versions lower than 1.7 use SourceIndex#from_gems_in
      source_index_class = (class << Gem::SourceIndex ; self ; end)
      source_index_class.send(:remove_method, :from_gems_in)
      source_index_class.send(:define_method, :from_gems_in) do |*args|
        source_index = Gem::SourceIndex.new
        source_index.spec_dirs = *args
        source_index.add_specs(*specs)
        source_index
      end
    end

    def stub_source_index170(specs)
      Gem::SourceIndex.send(:alias_method, :old_initialize, :initialize)
      Gem::SourceIndex.send(:define_method, :initialize) do |*args|
        @gems = {}
        # You're looking at this thinking: Oh! This is how I make those
        # rubygems deprecations go away!
        #
        # You'd be correct BUT using of this method in production code
        # must be approved by the rubygems team itself!
        #
        # This is your warning. If you use this and don't have approval
        # we can't protect you.
        #
        Deprecate.skip_during do
          self.spec_dirs = *args
          add_specs(*specs)
        end
      end
    end

    # Used to make bin stubs that are not created by bundler work
    # under bundler. The new Gem.bin_path only considers gems in
    # +specs+
    def replace_bin_path(specs)
      gem_class = (class << Gem ; self ; end)
      gem_class.send(:remove_method, :bin_path)
      gem_class.send(:define_method, :bin_path) do |name, *args|
        exec_name = args.first

        if exec_name == 'bundle'
          return ENV['BUNDLE_BIN_PATH']
        end

        spec = nil

        if exec_name
          spec = specs.find { |s| s.executables.include?(exec_name) }
          spec or raise Gem::Exception, "can't find executable #{exec_name}"
        else
          spec = specs.find  { |s| s.name == name }
          exec_name = spec.default_executable or raise Gem::Exception, "no default executable for #{spec.full_name}"
        end

        gem_bin = File.join(spec.full_gem_path, spec.bindir, exec_name)
        gem_from_path_bin = File.join(File.dirname(spec.loaded_from), spec.bindir, exec_name)
        File.exist?(gem_bin) ? gem_bin : gem_from_path_bin
      end
    end

    # Because Bundler has a static view of what specs are available,
    # we don't #refresh, so stub it out.
    def replace_refresh
      gem_class = (class << Gem ; self ; end)
      gem_class.send(:remove_method, :refresh)
      gem_class.send(:define_method, :refresh) { }
    end

    # Replace or hook into Rubygems to provide a bundlerized view
    # of the world.
    def replace_entrypoints(specs)
      reverse_rubygems_kernel_mixin

      replace_gem(specs)

      stub_rubygems(specs)

      replace_bin_path(specs)
      replace_refresh

      Gem.clear_paths
    end

    # This backports the correct segment generation code from Rubygems 1.4+
    # by monkeypatching it into the method in Rubygems 1.3.6 and 1.3.7.
    def backport_segment_generation
      Gem::Version.send(:define_method, :segments) do
        @segments ||= @version.scan(/[0-9]+|[a-z]+/i).map do |s|
          /^\d+$/ =~ s ? s.to_i : s
        end
      end
    end

    # This backport fixes the marshaling of @segments.
    def backport_yaml_initialize
      Gem::Version.send(:define_method, :yaml_initialize) do |tag, map|
        @version = map['version']
        @segments = nil
        @hash = nil
      end
    end

    # This backports base_dir which replaces installation path
    # Rubygems 1.8+
    def backport_base_dir
      Gem::Specification.send(:define_method, :base_dir) do
        return Gem.dir unless loaded_from
        File.dirname File.dirname loaded_from
      end
    end

    def backport_cache_file
      Gem::Specification.send(:define_method, :cache_dir) do
        @cache_dir ||= File.join base_dir, "cache"
      end

      Gem::Specification.send(:define_method, :cache_file) do
        @cache_file ||= File.join cache_dir, "#{full_name}.gem"
      end
    end

    def backport_spec_file
      Gem::Specification.send(:define_method, :spec_dir) do
        @spec_dir ||= File.join base_dir, "specifications"
      end

      Gem::Specification.send(:define_method, :spec_file) do
        @spec_file ||= File.join spec_dir, "#{full_name}.gemspec"
      end
    end

    # Rubygems 1.4 through 1.6
    class Legacy < RubygemsIntegration
      def initialize
        super
        backport_base_dir
        backport_cache_file
        backport_spec_file
        backport_yaml_initialize
      end

      def stub_rubygems(specs)
        stub_source_index137(specs)
      end

      def all_specs
        Gem.source_index.gems.values
      end

      def find_name(name)
        Gem.source_index.find_name(name)
      end
    end

    # Rubygems versions 1.3.6 and 1.3.7
    class Ancient < Legacy
      def initialize
        super
        backport_segment_generation
      end
    end

    # Rubygems 1.7
    class Transitional < Legacy
      def stub_rubygems(specs)
        stub_source_index170(specs)
      end
    end

    # Rubygems ~> 1.8.5
    class Modern < RubygemsIntegration
      def stub_rubygems(specs)
        Gem::Specification.all = specs

        Gem.post_reset {
          Gem::Specification.all = specs
        }

        stub_source_index170(specs)
      end

      def all_specs
        Gem::Specification.to_a
      end

      def find_name(name)
        Gem::Specification.find_all_by_name name
      end
    end

    # Rubygems 1.8.0 to 1.8.4
    class AlmostModern < Modern
      # Rubygems [>= 1.8.0, < 1.8.5] has a bug that changes Gem.dir whenever
      # you call Gem::Installer#install with an :install_dir set. We have to
      # change it back for our sudo mode to work.
      def preserve_paths
        old_dir, old_path = gem_dir, gem_path
        yield
        Gem.use_paths(old_dir, old_path)
      end
    end

    # Rubygems 2.0
    class Future < RubygemsIntegration
      def stub_rubygems(specs)
        Gem::Specification.all = specs

        Gem.post_reset {
          Gem::Specification.all = specs
        }
      end

      def all_specs
        Gem::Specification.to_a
      end

      def find_name(name)
        Gem::Specification.find_all_by_name name
      end

      def fetch_specs(source, name)
        path = source + "#{name}.#{Gem.marshal_version}.gz"
        string = Gem::RemoteFetcher.fetcher.fetch_path(path)
        Bundler.load_marshal(string)
      rescue Gem::RemoteFetcher::FetchError => e
        # it's okay for prerelease to fail
        raise e unless name == "prerelease_specs"
      end

      def fetch_all_remote_specs
        # Since SpecFetcher now returns NameTuples, we just fetch directly
        # and unmarshal the array ourselves.
        hash = {}

        Gem.sources.each do |source|
          source = URI.parse(source.to_s) unless source.is_a?(URI)
          hash[source] = fetch_specs(source, "specs")

          pres = fetch_specs(source, "prerelease_specs")
          hash[source].push(*pres) if pres && !pres.empty?
        end

        hash
      end

      def gem_from_path(path, policy = nil)
        require 'rubygems/package'
        p = Gem::Package.new(path)
        p.security_policy = policy if policy
        return p
      end

      def build(spec)
        require 'rubygems/package'
        Gem::Package.build(spec)
      end

    end

  end

  if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.99.99')
    @rubygems = RubygemsIntegration::Future.new
  elsif Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.8.5')
    @rubygems = RubygemsIntegration::Modern.new
  elsif Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.8.0')
    @rubygems = RubygemsIntegration::AlmostModern.new
  elsif Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.7.0')
    @rubygems = RubygemsIntegration::Transitional.new
  elsif Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.4.0')
    @rubygems = RubygemsIntegration::Legacy.new
  else # Rubygems 1.3.6 and 1.3.7
    @rubygems = RubygemsIntegration::Ancient.new
  end

  class << self
    attr_reader :rubygems
  end
end
