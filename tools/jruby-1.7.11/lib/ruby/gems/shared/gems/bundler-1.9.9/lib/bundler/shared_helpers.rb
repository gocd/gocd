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
      gemfile = default_gemfile

      case gemfile.basename.to_s
      when 'gems.rb' then Pathname.new(gemfile.sub(/.rb$/, '.locked'))
      else Pathname.new("#{gemfile}.lock")
      end
    end

    def default_bundle_dir
      bundle_dir = find_directory(".bundle")
      return nil unless bundle_dir

      global_bundle_dir = File.join(Bundler.rubygems.user_home, ".bundle")
      return nil if bundle_dir == global_bundle_dir

      Pathname.new(bundle_dir)
    end

    def in_bundle?
      find_gemfile
    end

    def chdir(dir, &blk)
      Bundler.rubygems.ext_lock.synchronize do
        Dir.chdir dir, &blk
      end
    end

    def pwd
      Bundler.rubygems.ext_lock.synchronize do
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

    def set_bundle_environment
      # Set PATH
      paths = (ENV["PATH"] || "").split(File::PATH_SEPARATOR)
      paths.unshift "#{Bundler.bundle_path}/bin"
      ENV["PATH"] = paths.uniq.join(File::PATH_SEPARATOR)

      # Set RUBYOPT
      rubyopt = [ENV["RUBYOPT"]].compact
      if rubyopt.empty? || rubyopt.first !~ /-rbundler\/setup/
        rubyopt.unshift %|-rbundler/setup|
        ENV["RUBYOPT"] = rubyopt.join(' ')
      end

      # Set RUBYLIB
      rubylib = (ENV["RUBYLIB"] || "").split(File::PATH_SEPARATOR)
      rubylib.unshift File.expand_path('../..', __FILE__)
      ENV["RUBYLIB"] = rubylib.uniq.join(File::PATH_SEPARATOR)
    end

  private

    def find_gemfile
      given = ENV['BUNDLE_GEMFILE']
      return given if given && !given.empty?

      find_file('Gemfile', 'gems.rb')
    end

    def find_file(*names)
      search_up(*names) {|filename|
        return filename if File.file?(filename)
      }
    end

    def find_directory(*names)
      search_up(*names) do |dirname|
        return dirname if File.directory?(dirname)
      end
    end

    def search_up(*names)
      previous = nil
      current  = File.expand_path(SharedHelpers.pwd)

      until !File.directory?(current) || current == previous
        if ENV['BUNDLE_SPEC_RUN']
          # avoid stepping above the tmp directory when testing
          return nil if File.file?(File.join(current, 'bundler.gemspec'))
        end

        names.each do |name|
          filename = File.join(current, name)
          yield filename
        end
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
