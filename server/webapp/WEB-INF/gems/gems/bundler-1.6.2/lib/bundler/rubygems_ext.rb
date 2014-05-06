require 'pathname'

if defined?(Gem::QuickLoader)
  # Gem Prelude makes me a sad panda :'(
  Gem::QuickLoader.load_full_rubygems_library
end

require 'rubygems'
require 'rubygems/specification'
require 'bundler/match_platform'

module Gem
  @loaded_stacks = Hash.new { |h,k| h[k] = [] }

  class Specification
    attr_accessor :source_uri, :location, :relative_loaded_from

    remove_method :source if instance_methods(false).include?(:source)
    attr_accessor :source

    alias_method :rg_full_gem_path, :full_gem_path
    alias_method :rg_loaded_from,   :loaded_from

    def full_gem_path
      source.respond_to?(:path) ?
        Pathname.new(loaded_from).dirname.expand_path(Bundler.root).to_s :
        rg_full_gem_path
    end

    def loaded_from
      relative_loaded_from ?
        source.path.join(relative_loaded_from).to_s :
        rg_loaded_from
    end

    def load_paths
      return full_require_paths if respond_to?(:full_require_paths)

      require_paths.map do |require_path|
        if require_path.include?(full_gem_path)
          require_path
        else
          File.join(full_gem_path, require_path)
        end
      end
    end

    if method_defined?(:extension_dir)
      alias_method :rg_extension_dir, :extension_dir
      def extension_dir
        @extension_dir ||= source.respond_to?(:extension_dir_name) ?
          File.expand_path(File.join(extensions_dir, source.extension_dir_name)) :
          rg_extension_dir
      end
    end

    # RubyGems 1.8+ used only.
    remove_method :gem_dir if instance_methods(false).include?(:gem_dir)
    def gem_dir
      full_gem_path
    end

    def groups
      @groups ||= []
    end

    def git_version
      return unless loaded_from && source.is_a?(Bundler::Source::Git)
      " #{source.revision[0..6]}"
    end

    def to_gemfile(path = nil)
      gemfile = "source :gemcutter\n"
      gemfile << dependencies_to_gemfile(nondevelopment_dependencies)
      unless development_dependencies.empty?
        gemfile << "\n"
        gemfile << dependencies_to_gemfile(development_dependencies, :development)
      end
      gemfile
    end

    def nondevelopment_dependencies
      dependencies - development_dependencies
    end

  private

    def dependencies_to_gemfile(dependencies, group = nil)
      gemfile = ''
      if dependencies.any?
        gemfile << "group :#{group} do\n" if group
        dependencies.each do |dependency|
          gemfile << '  ' if group
          gemfile << %|gem "#{dependency.name}"|
          req = dependency.requirements_list.first
          gemfile << %|, "#{req}"| if req
          gemfile << "\n"
        end
        gemfile << "end\n" if group
      end
      gemfile
    end

  end

  class Dependency
    attr_accessor :source, :groups

    alias eql? ==

    def encode_with(coder)
      to_yaml_properties.each do |ivar|
        coder[ivar.to_s.sub(/^@/, '')] = instance_variable_get(ivar)
      end
    end

    def to_yaml_properties
      instance_variables.reject { |p| ["@source", "@groups"].include?(p.to_s) }
    end

    def to_lock
      out = "  #{name}"
      unless requirement == Gem::Requirement.default
        reqs = requirement.requirements.map{|o,v| "#{o} #{v}" }.sort.reverse
        out << " (#{reqs.join(', ')})"
      end
      out
    end

    # Backport of performance enhancement added to Rubygems 1.4
    def matches_spec?(spec)
      # name can be a Regexp, so use ===
      return false unless name === spec.name
      return true  if requirement.none?

      requirement.satisfied_by?(spec.version)
    end unless allocate.respond_to?(:matches_spec?)
  end

  class Requirement
    # Backport of performance enhancement added to Rubygems 1.4
    def none?
      @none ||= (to_s == ">= 0")
    end unless allocate.respond_to?(:none?)
  end

  class Platform
    JAVA  = Gem::Platform.new('java') unless defined?(JAVA)
    MSWIN = Gem::Platform.new('mswin32') unless defined?(MSWIN)
    MINGW = Gem::Platform.new('x86-mingw32') unless defined?(MINGW)
    X64_MINGW = Gem::Platform.new('x64-mingw32') unless defined?(X64_MINGW)

    undef_method :hash if method_defined? :hash
    def hash
      @cpu.hash ^ @os.hash ^ @version.hash
    end

    undef_method :eql? if method_defined? :eql?
    alias eql? ==
  end
end

module Gem
  class Specification
    include ::Bundler::MatchPlatform
  end
end
