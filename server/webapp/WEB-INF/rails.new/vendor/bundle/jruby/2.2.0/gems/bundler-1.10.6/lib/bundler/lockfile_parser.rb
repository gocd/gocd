require "strscan"

# Some versions of the Bundler 1.1 RC series introduced corrupted
# lockfiles. There were two major problems:
#
# * multiple copies of the same GIT section appeared in the lockfile
# * when this happened, those sections got multiple copies of gems
#   in those sections.
#
# As a result, Bundler 1.1 contains code that fixes the earlier
# corruption. We will remove this fix-up code in Bundler 1.2.

module Bundler
  class LockfileParser
    attr_reader :sources, :dependencies, :specs, :platforms, :bundler_version

    BUNDLED      = "BUNDLED WITH"
    DEPENDENCIES = "DEPENDENCIES"
    PLATFORMS    = "PLATFORMS"
    GIT          = "GIT"
    GEM          = "GEM"
    PATH         = "PATH"
    SPECS        = "  specs:"
    OPTIONS      = /^  ([a-z]+): (.*)$/i
    SOURCE       = [GIT, GEM, PATH]

    def initialize(lockfile)
      @platforms    = []
      @sources      = []
      @dependencies = []
      @state        = nil
      @specs        = {}

      @rubygems_aggregate = Source::Rubygems.new

      if lockfile.match(/<<<<<<<|=======|>>>>>>>|\|\|\|\|\|\|\|/)
        raise LockfileError, "Your Gemfile.lock contains merge conflicts.\n" \
          "Run `git checkout HEAD -- Gemfile.lock` first to get a clean lock."
      end

      lockfile.split(/(?:\r?\n)+/).each do |line|
        if SOURCE.include?(line)
          @state = :source
          parse_source(line)
        elsif line == DEPENDENCIES
          @state = :dependency
        elsif line == PLATFORMS
          @state = :platform
        elsif line == BUNDLED
          @state = :bundled_with
        elsif line =~ /^[^\s]/
          @state = nil
        elsif @state
          send("parse_#{@state}", line)
        end
      end
      @sources << @rubygems_aggregate
      @specs = @specs.values
      warn_for_outdated_bundler_version
    rescue ArgumentError => e
      Bundler.ui.debug(e)
      raise LockfileError, "Your lockfile is unreadable. Run `rm Gemfile.lock` " \
        "and then `bundle install` to generate a new lockfile."
    end

    def warn_for_outdated_bundler_version
      return unless bundler_version
      prerelease_text = bundler_version.prerelease? ? " --pre" : ""
      current_version = Gem::Version.create(Bundler::VERSION)
      case current_version.segments.first <=> bundler_version.segments.first
      when -1
        raise LockfileError, "You must use Bundler #{bundler_version.segments.first} or greater with this lockfile."
      when 0
        if current_version < bundler_version
          Bundler.ui.warn "Warning: the running version of Bundler is older " \
               "than the version that created the lockfile. We suggest you " \
               "upgrade to the latest version of Bundler by running `gem " \
               "install bundler#{prerelease_text}`.\n"
        end
      end
    end

  private

    TYPES = {
      GIT  => Bundler::Source::Git,
      GEM  => Bundler::Source::Rubygems,
      PATH => Bundler::Source::Path,
    }

    def parse_source(line)
      case line
      when GIT, GEM, PATH
        @current_source = nil
        @opts, @type = {}, line
      when SPECS
        case @type
        when PATH
          @current_source = TYPES[@type].from_lock(@opts)
          @sources << @current_source
        when GIT
          @current_source = TYPES[@type].from_lock(@opts)
          # Strip out duplicate GIT sections
          if @sources.include?(@current_source)
            @current_source = @sources.find { |s| s == @current_source }
          else
            @sources << @current_source
          end
        when GEM
          Array(@opts["remote"]).each do |url|
            @rubygems_aggregate.add_remote(url)
          end
          @current_source = @rubygems_aggregate
        end
      when OPTIONS
        value = $2
        value = true if value == "true"
        value = false if value == "false"

        key = $1

        if @opts[key]
          @opts[key] = Array(@opts[key])
          @opts[key] << value
        else
          @opts[key] = value
        end
      else
        parse_spec(line)
      end
    end

    NAME_VERSION = '(?! )(.*?)(?: \(([^-]*)(?:-(.*))?\))?'
    NAME_VERSION_2 = %r{^ {2}#{NAME_VERSION}(!)?$}
    NAME_VERSION_4 = %r{^ {4}#{NAME_VERSION}$}
    NAME_VERSION_6 = %r{^ {6}#{NAME_VERSION}$}

    def parse_dependency(line)
      if line =~ NAME_VERSION_2
        name, version, pinned = $1, $2, $4
        version = version.split(",").map { |d| d.strip } if version

        dep = Bundler::Dependency.new(name, version)

        if pinned && dep.name != 'bundler'
          spec = @specs.find {|k, v| v.name == dep.name }
          dep.source = spec.last.source if spec

          # Path sources need to know what the default name / version
          # to use in the case that there are no gemspecs present. A fake
          # gemspec is created based on the version set on the dependency
          # TODO: Use the version from the spec instead of from the dependency
          if version && version.size == 1 && version.first =~ /^\s*= (.+)\s*$/ && dep.source.is_a?(Bundler::Source::Path)
            dep.source.name    = name
            dep.source.version = $1
          end
        end

        @dependencies << dep
      end
    end

    def parse_spec(line)
      if line =~ NAME_VERSION_4
        name, version = $1, Gem::Version.new($2)
        platform = $3 ? Gem::Platform.new($3) : Gem::Platform::RUBY
        @current_spec = LazySpecification.new(name, version, platform)
        @current_spec.source = @current_source

        # Avoid introducing multiple copies of the same spec (caused by
        # duplicate GIT sections)
        @specs[@current_spec.identifier] ||= @current_spec
      elsif line =~ NAME_VERSION_6
        name, version = $1, $2
        version = version.split(',').map { |d| d.strip } if version
        dep = Gem::Dependency.new(name, version)
        @current_spec.dependencies << dep
      end
    end

    def parse_platform(line)
      if line =~ /^  (.*)$/
        @platforms << Gem::Platform.new($1)
      end
    end

    def parse_bundled_with(line)
      line = line.strip
      if Gem::Version.correct?(line)
        @bundler_version = Gem::Version.create(line)
      end
    end

  end
end
