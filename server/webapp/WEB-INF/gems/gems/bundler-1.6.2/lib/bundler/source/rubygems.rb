require 'uri'
require 'rubygems/user_interaction'
require 'rubygems/spec_fetcher'

module Bundler
  class Source
    class Rubygems < Source
      API_REQUEST_LIMIT = 100 # threshold for switching back to the modern index instead of fetching every spec

      attr_reader :remotes, :caches
      attr_accessor :dependency_names

      def initialize(options = {})
        @options = options
        @remotes = (options["remotes"] || []).map { |r| normalize_uri(r) }
        @fetchers = {}
        @dependency_names = []
        @allow_remote = false
        @allow_cached = false
        @caches = [Bundler.app_cache, *Bundler.rubygems.gem_cache]
      end

      def remote!
        @allow_remote = true
      end

      def cached!
        @allow_cached = true
      end

      def hash
        Rubygems.hash
      end

      def eql?(o)
        o.is_a?(Rubygems)
      end

      alias == eql?

      def options
        { "remotes" => @remotes.map { |r| r.to_s } }
      end

      def self.from_lock(options)
        s = new(options)
        Array(options["remote"]).each { |r| s.add_remote(r) }
        s
      end

      def to_lock
        out = "GEM\n"
        out << remotes.map {|r| "  remote: #{r}\n" }.join
        out << "  specs:\n"
      end

      def to_s
        remote_names = self.remotes.map { |r| r.to_s }.join(', ')
        "rubygems repository #{remote_names}"
      end
      alias_method :name, :to_s

      def specs
        @specs ||= fetch_specs
      end

      def install(spec)
        return ["Using #{version_message(spec)}", nil] if installed_specs[spec].any?

        # Download the gem to get the spec, because some specs that are returned
        # by rubygems.org are broken and wrong.
        if spec.source_uri
          s = Bundler.rubygems.spec_from_gem(fetch_gem(spec), Bundler.settings["trust-policy"])
          spec.__swap__(s)
        end

        path = cached_gem(spec)
        if Bundler.requires_sudo?
          install_path = Bundler.tmp(spec.full_name)
          bin_path     = install_path.join("bin")
        else
          install_path = Bundler.rubygems.gem_dir
          bin_path     = Bundler.system_bindir
        end

        installed_spec = nil
        Bundler.rubygems.preserve_paths do
          installed_spec = Bundler::GemInstaller.new(path,
            :install_dir         => install_path.to_s,
            :bin_dir             => bin_path.to_s,
            :ignore_dependencies => true,
            :wrappers            => true,
            :env_shebang         => true
          ).install
        end

        # SUDO HAX
        if Bundler.requires_sudo?
          Bundler.rubygems.repository_subdirectories.each do |name|
            src = File.join(install_path, name, "*")
            dst = File.join(Bundler.rubygems.gem_dir, name)
            if name == "extensions" && Dir.glob(src).any?
              src = File.join(src, "*/*")
              ext_src = Dir.glob(src).first
              ext_src.gsub!(src[0..-6], '')
              dst = File.dirname(File.join(dst, ext_src))
            end
            Bundler.mkdir_p dst
            Bundler.sudo "cp -R #{src} #{dst}" if Dir[src].any?
          end

          spec.executables.each do |exe|
            Bundler.mkdir_p Bundler.system_bindir
            Bundler.sudo "cp -R #{install_path}/bin/#{exe} #{Bundler.system_bindir}/"
          end
        end

        spec.loaded_from = "#{Bundler.rubygems.gem_dir}/specifications/#{spec.full_name}.gemspec"
        installed_spec.loaded_from = spec.loaded_from
        ["Installing #{version_message(spec)}", spec.post_install_message]
      ensure
        if install_path && Bundler.requires_sudo?
          FileUtils.remove_entry_secure(install_path)
        end
      end

      def cache(spec, custom_path = nil)
        if builtin_gem?(spec)
          remote_spec = remote_specs.search(spec).first
          cached_path = fetch_gem(remote_spec)
        else
          cached_path = cached_gem(spec)
        end
        raise GemNotFound, "Missing gem file '#{spec.full_name}.gem'." unless cached_path
        return if File.dirname(cached_path) == Bundler.app_cache.to_s
        Bundler.ui.info "  * #{File.basename(cached_path)}"
        FileUtils.cp(cached_path, Bundler.app_cache(custom_path))
      end

      def add_remote(source)
        @remotes << normalize_uri(source)
      end

      def replace_remotes(source)
        return false if source.remotes == @remotes

        @remotes = []
        source.remotes.each do |r|
          add_remote r.to_s
        end

        true
      end

    private

      def cached_gem(spec)
        possibilities = @caches.map { |p| "#{p}/#{spec.file_name}" }
        cached_gem = possibilities.find { |p| File.exist?(p) }
        unless cached_gem
          raise Bundler::GemNotFound, "Could not find #{spec.file_name} for installation"
        end
        cached_gem
      end

      def normalize_uri(uri)
        uri = uri.to_s
        uri = "#{uri}/" unless uri =~ %r'/$'
        uri = URI(uri)
        raise ArgumentError, "The source must be an absolute URI" unless uri.absolute?
        uri
      end

      def fetch_specs
        # remote_specs usually generates a way larger Index than the other
        # sources, and large_idx.use small_idx is way faster than
        # small_idx.use large_idx.
        if @allow_remote
          idx = remote_specs.dup
        else
          idx = Index.new
        end
        idx.use(cached_specs, :override_dupes) if @allow_cached || @allow_remote
        idx.use(installed_specs, :override_dupes)
        idx
      end

      def installed_specs
        @installed_specs ||= begin
          idx = Index.new
          have_bundler = false
          Bundler.rubygems.all_specs.reverse.each do |spec|
            next if spec.name == 'bundler' && spec.version.to_s != VERSION
            have_bundler = true if spec.name == 'bundler'
            spec.source = self
            idx << spec
          end

          # Always have bundler locally
          unless have_bundler
           # We're running bundler directly from the source
           # so, let's create a fake gemspec for it (it's a path)
           # gemspec
           bundler = Gem::Specification.new do |s|
             s.name     = 'bundler'
             s.version  = VERSION
             s.platform = Gem::Platform::RUBY
             s.source   = self
             s.authors  = ["bundler team"]
             s.loaded_from = File.expand_path("..", __FILE__)
           end
           idx << bundler
          end
          idx
        end
      end

      def cached_specs
        @cached_specs ||= begin
          idx = installed_specs.dup

          path = Bundler.app_cache
          Dir["#{path}/*.gem"].each do |gemfile|
            next if gemfile =~ /^bundler\-[\d\.]+?\.gem/
            s ||= Bundler.rubygems.spec_from_gem(gemfile)
            s.source = self
            idx << s
          end
        end

        idx
      end

      def remote_specs
        @remote_specs ||= begin
          old = Bundler.rubygems.sources
          idx = Index.new

          fetchers       = remotes.map { |uri| Bundler::Fetcher.new(uri) }
          api_fetchers   = fetchers.select { |f| f.use_api }
          index_fetchers = fetchers - api_fetchers

          # gather lists from non-api sites
          index_fetchers.each do |f|
            Bundler.ui.info "Fetching source index from #{f.uri}"
            idx.use f.specs(nil, self)
          end
          return idx if api_fetchers.empty?

          # because ensuring we have all the gems we need involves downloading
          # the gemspecs of those gems, if the non-api sites contain more than
          # about 100 gems, we just treat all sites as non-api for speed.
          allow_api = idx.size < API_REQUEST_LIMIT && dependency_names.size < API_REQUEST_LIMIT

          if allow_api
            api_fetchers.each do |f|
              Bundler.ui.info "Fetching gem metadata from #{f.uri}", Bundler.ui.debug?
              idx.use f.specs(dependency_names, self)
              Bundler.ui.info "" if !Bundler.ui.debug? # new line now that the dots are over
            end

            if api_fetchers.all?{|f| f.use_api }
              # it's possible that gems from one source depend on gems from some
              # other source, so now we download gemspecs and iterate over those
              # dependencies, looking for gems we don't have info on yet.
              unmet = idx.unmet_dependency_names

              # if there are any cross-site gems we missed, get them now
              api_fetchers.each do |f|
                Bundler.ui.info "Fetching additional metadata from #{f.uri}", Bundler.ui.debug?
                idx.use f.specs(unmet, self)
                Bundler.ui.info "" if !Bundler.ui.debug? # new line now that the dots are over
              end if unmet.any?
            else
              allow_api = false
            end
          end

          if !allow_api
            api_fetchers.each do |f|
              Bundler.ui.info "Fetching source index from #{f.uri}"
              idx.use f.specs(nil, self)
            end
          end

          return idx
        ensure
          Bundler.rubygems.sources = old
        end
      end

      def fetch_gem(spec)
        return false unless spec.source_uri
        Fetcher.download_gem_from_uri(spec, spec.source_uri)
      end

      def builtin_gem?(spec)
        # Ruby 2.1, where all included gems have this summary
        return true if spec.summary =~ /is bundled with Ruby/

        # Ruby 2.0, where gemspecs are stored in specifications/default/
        spec.loaded_from && spec.loaded_from.include?("specifications/default/")
      end
    end
  end
end
