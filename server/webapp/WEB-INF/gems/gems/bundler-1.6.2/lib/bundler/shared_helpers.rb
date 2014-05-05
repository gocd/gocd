require 'pathname'
require 'rubygems'

require 'bundler/constants'
require 'bundler/rubygems_integration'
require 'bundler/current_ruby'

module Gem
  class Dependency
    if !instance_methods.map { |m| m.to_s }.include?("requirement")
      def requirement
        version_requirements
      end
    end
  end
end

module Bundler
  module SharedHelpers
    attr_accessor :gem_loaded

    def default_gemfile
      gemfile = find_gemfile
      raise GemfileNotFound, "Could not locate Gemfile" unless gemfile
      Pathname.new(gemfile)
    end

    def default_lockfile
      Pathname.new("#{default_gemfile}.lock")
    end

    def in_bundle?
      find_gemfile
    end

    if Bundler.current_ruby.mswin? || Bundler.current_ruby.jruby?
      require 'monitor'
      @chdir_monitor = Monitor.new
      def chdir(dir, &blk)
        @chdir_monitor.synchronize do
          Dir.chdir dir, &blk
        end
      end

      def pwd
        @chdir_monitor.synchronize do
          Dir.pwd
        end
      end
    else
      def chdir(dir, &blk)
        Dir.chdir dir, &blk
      end

      def pwd
        Dir.pwd
      end
    end

    def with_clean_git_env(&block)
      keys    = %w[GIT_DIR GIT_WORK_TREE]
      old_env = keys.inject({}) do |h, k|
        h.update(k => ENV[k])
      end

      keys.each {|key| ENV.delete(key) }

      block.call
    ensure
      keys.each {|key| ENV[key] = old_env[key] }
    end

  private

    def find_gemfile
      given = ENV['BUNDLE_GEMFILE']
      return given if given && !given.empty?

      previous = nil
      current  = File.expand_path(SharedHelpers.pwd)

      until !File.directory?(current) || current == previous
        if ENV['BUNDLE_SPEC_RUN']
          # avoid stepping above the tmp directory when testing
          return nil if File.file?(File.join(current, 'bundler.gemspec'))
        end

        # otherwise return the Gemfile if it's there
        filename = File.join(current, 'Gemfile')
        return filename if File.file?(filename)
        current, previous = File.expand_path("..", current), current
      end
    end

    def clean_load_path
      # handle 1.9 where system gems are always on the load path
      if defined?(::Gem)
        me = File.expand_path("../../", __FILE__)
        $LOAD_PATH.reject! do |p|
          next if File.expand_path(p) =~ /^#{Regexp.escape(me)}/
          p != File.dirname(__FILE__) &&
            Bundler.rubygems.gem_path.any?{|gp| p =~ /^#{Regexp.escape(gp)}/ }
        end
        $LOAD_PATH.uniq!
      end
    end

    extend self
  end
end
