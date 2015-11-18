require 'childprocess/errors'
require 'childprocess/abstract_process'
require 'childprocess/abstract_io'
require "fcntl"

module ChildProcess

  @posix_spawn = false

  class << self
    def new(*args)
      case os
      when :macosx, :linux, :solaris, :bsd, :cygwin, :aix
        if posix_spawn?
          Unix::PosixSpawnProcess.new(args)
        elsif jruby?
          JRuby::Process.new(args)
        else
          Unix::ForkExecProcess.new(args)
        end
      when :windows
        Windows::Process.new(args)
      else
        raise Error, "unsupported platform #{platform_name.inspect}"
      end
    end
    alias_method :build, :new

    def platform
      if RUBY_PLATFORM == "java"
        :jruby
      elsif defined?(RUBY_ENGINE) && RUBY_ENGINE == "ironruby"
        :ironruby
      else
        os
      end
    end

    def platform_name
      @platform_name ||= "#{arch}-#{os}"
    end

    def unix?
      !windows?
    end

    def linux?
      os == :linux
    end

    def jruby?
      platform == :jruby
    end

    def windows?
      os == :windows
    end

    def posix_spawn?
      enabled = @posix_spawn || %w[1 true].include?(ENV['CHILDPROCESS_POSIX_SPAWN'])
      return false unless enabled

      require 'ffi'
      begin
        require "childprocess/unix/platform/#{ChildProcess.platform_name}"
      rescue LoadError
        raise ChildProcess::MissingPlatformError
      end

      require "childprocess/unix/lib"
      require 'childprocess/unix/posix_spawn_process'

      true
    rescue ChildProcess::MissingPlatformError => ex
      warn_once ex.message
      false
    end

    #
    # Set this to true to enable experimental use of posix_spawn.
    #

    def posix_spawn=(bool)
      @posix_spawn = bool
    end

    def os
      @os ||= (
        require "rbconfig"
        host_os = RbConfig::CONFIG['host_os'].downcase

        case host_os
        when /linux/
          :linux
        when /darwin|mac os/
          :macosx
        when /mswin|msys|mingw32/
          :windows
        when /cygwin/
          :cygwin
        when /solaris|sunos/
          :solaris
        when /bsd/
          :bsd
        when /aix/
          :aix
        else
          raise Error, "unknown os: #{host_os.inspect}"
        end
      )
    end

    def arch
      @arch ||= (
        host_cpu = RbConfig::CONFIG['host_cpu'].downcase
        case host_cpu
        when /i[3456]86/
          # Darwin always reports i686, even when running in 64bit mod
          if os == :macosx && 0xfee1deadbeef.is_a?(Fixnum)
            "x86_64"
          else
            "i386"
          end
        when /amd64|x86_64/
          "x86_64"
        when /ppc|powerpc/
          "powerpc"
        else
          host_cpu
        end
      )
    end

    #
    # By default, a child process will inherit open file descriptors from the
    # parent process. This helper provides a cross-platform way of making sure
    # that doesn't happen for the given file/io.
    #

    def close_on_exec(file)
      if file.respond_to?(:close_on_exec=)
        file.close_on_exec = true
      elsif file.respond_to?(:fcntl) && defined?(Fcntl::FD_CLOEXEC)
        file.fcntl Fcntl::F_SETFD, Fcntl::FD_CLOEXEC

        if jruby? && posix_spawn?
          # on JRuby, the fcntl call above apparently isn't enough when
          # we're launching the process through posix_spawn.
          fileno = JRuby.posix_fileno_for(file)
          Unix::Lib.fcntl fileno, Fcntl::F_SETFD, Fcntl::FD_CLOEXEC
        end
      elsif windows?
        Windows::Lib.dont_inherit file
      else
        raise Error, "not sure how to set close-on-exec for #{file.inspect} on #{platform_name.inspect}"
      end
    end

    private

    def warn_once(msg)
      @warnings ||= {}

      unless @warnings[msg]
        @warnings[msg] = true
        $stderr.puts msg
      end
    end

  end # class << self
end # ChildProcess

require 'jruby' if ChildProcess.jruby?

require 'childprocess/unix'    if ChildProcess.unix?
require 'childprocess/windows' if ChildProcess.windows?
require 'childprocess/jruby'   if ChildProcess.jruby?
