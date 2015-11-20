require 'rubygems/dependency'
require 'bundler/shared_helpers'
require 'bundler/rubygems_ext'

module Bundler
  class Dependency < Gem::Dependency
    attr_reader :autorequire
    attr_reader :groups
    attr_reader :platforms

    PLATFORM_MAP = {
      :ruby     => Gem::Platform::RUBY,
      :ruby_18  => Gem::Platform::RUBY,
      :ruby_19  => Gem::Platform::RUBY,
      :ruby_20  => Gem::Platform::RUBY,
      :ruby_21  => Gem::Platform::RUBY,
      :ruby_22  => Gem::Platform::RUBY,
      :mri      => Gem::Platform::RUBY,
      :mri_18   => Gem::Platform::RUBY,
      :mri_19   => Gem::Platform::RUBY,
      :mri_20   => Gem::Platform::RUBY,
      :mri_21   => Gem::Platform::RUBY,
      :mri_22   => Gem::Platform::RUBY,
      :rbx      => Gem::Platform::RUBY,
      :jruby    => Gem::Platform::JAVA,
      :jruby_18 => Gem::Platform::JAVA,
      :jruby_19 => Gem::Platform::JAVA,
      :mswin    => Gem::Platform::MSWIN,
      :mswin_18 => Gem::Platform::MSWIN,
      :mswin_19 => Gem::Platform::MSWIN,
      :mswin_20 => Gem::Platform::MSWIN,
      :mswin_21 => Gem::Platform::MSWIN,
      :mswin64    => Gem::Platform::MSWIN64,
      :mswin64_19 => Gem::Platform::MSWIN64,
      :mswin64_20 => Gem::Platform::MSWIN64,
      :mswin64_21 => Gem::Platform::MSWIN64,
      :mingw    => Gem::Platform::MINGW,
      :mingw_18 => Gem::Platform::MINGW,
      :mingw_19 => Gem::Platform::MINGW,
      :mingw_20 => Gem::Platform::MINGW,
      :mingw_21 => Gem::Platform::MINGW,
      :mingw_22 => Gem::Platform::MINGW,
      :x64_mingw    => Gem::Platform::X64_MINGW,
      :x64_mingw_20 => Gem::Platform::X64_MINGW,
      :x64_mingw_21 => Gem::Platform::X64_MINGW,
      :x64_mingw_22 => Gem::Platform::X64_MINGW
    }.freeze

    def initialize(name, version, options = {}, &blk)
      type = options["type"] || :runtime
      super(name, version, type)

      @autorequire    = nil
      @groups         = Array(options["group"] || :default).map { |g| g.to_sym }
      @source         = options["source"]
      @platforms      = Array(options["platforms"])
      @env            = options["env"]
      @should_include = options.fetch("should_include", true)

      if options.key?('require')
        @autorequire = Array(options['require'] || [])
      end
    end

    def gem_platforms(valid_platforms)
      return valid_platforms if @platforms.empty?

      platforms = []
      @platforms.each do |p|
        platform = PLATFORM_MAP[p]
        next unless valid_platforms.include?(platform)
        platforms |= [platform]
      end
      platforms
    end

    def should_include?
      @should_include && current_env? && current_platform?
    end

    def current_env?
      return true unless @env
      if @env.is_a?(Hash)
        @env.all? do |key, val|
          ENV[key.to_s] && (val.is_a?(String) ? ENV[key.to_s] == val : ENV[key.to_s] =~ val)
        end
      else
        ENV[@env.to_s]
      end
    end

    def current_platform?
      return true if @platforms.empty?
      @platforms.any? { |p|
        Bundler.current_ruby.send("#{p}?")
      }
    end

    def to_lock
      out = super
      out << '!' if source
      out << "\n"
    end

    def specific?
      super
    rescue NoMethodError
      requirement != ">= 0"
    end
  end
end
