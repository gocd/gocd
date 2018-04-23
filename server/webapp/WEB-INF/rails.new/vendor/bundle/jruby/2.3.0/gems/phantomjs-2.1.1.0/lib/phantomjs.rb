require "phantomjs/version"
require 'fileutils'

module Phantomjs
  class UnknownPlatform < StandardError; end;

  class << self
    def available_platforms
      @available_platforms ||= []
    end

    def base_dir
       @base_dir ||= File.join(File.expand_path('~'), '.phantomjs', version)
    end

    def base_dir=(dir)
      @base_dir = dir
    end

    def version
      Phantomjs::VERSION.split('.')[0..-2].join('.')
    end

    def path
      @path ||= platform.phantomjs_path
    end

    def platform
      if platform = available_platforms.find {|p| p.useable? }
        platform.ensure_installed!
        platform
      else
        raise UnknownPlatform, "Could not find an appropriate PhantomJS library for your platform (#{RUBY_PLATFORM} :( Please install manually."
      end
    end

    # Removes the local phantomjs copy
    def implode!
      FileUtils.rm_rf File.join(File.expand_path('~'), '.phantomjs')
    end

    # Clears cached state. Primarily useful for testing.
    def reset!
      @base_dir = @path = nil
    end

    # Run phantomjs with the given arguments, and either
    # return the stdout or yield each line to the passed block.
    def run(*args, &block)
      IO.popen([path, *args]) do |io|
        block ? io.each(&block) : io.read
      end
    end
  end
end

require 'phantomjs/platform'
Phantomjs.available_platforms << Phantomjs::Platform::Linux32
Phantomjs.available_platforms << Phantomjs::Platform::Linux64
Phantomjs.available_platforms << Phantomjs::Platform::OsX
Phantomjs.available_platforms << Phantomjs::Platform::Win32
