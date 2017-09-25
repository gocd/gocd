require 'ruby-debug-ide/helper'
require 'delegate'

module Debugger

  class Command < SimpleDelegator # :nodoc:
    SubcmdStruct=Struct.new(:name, :min, :short_help, :long_help) unless
      defined?(SubcmdStruct)

    # Find param in subcmds. param id downcased and can be abbreviated
    # to the minimum length listed in the subcommands
    def find(subcmds, param)
      param.downcase!
      for try_subcmd in subcmds do
        if (param.size >= try_subcmd.min) and
            (try_subcmd.name[0..param.size-1] == param)
          return try_subcmd
        end
      end
      return nil
    end

    class << self
      def commands
        @commands ||= []
      end
      
      DEF_OPTIONS = {
        :event => true, 
        :control => false, 
        :unknown => false,
        :need_context => false,
      }
      
      def inherited(klass)
        DEF_OPTIONS.each do |o, v|
          klass.options[o] = v if klass.options[o].nil?
        end
        commands << klass
      end 

      def load_commands
        dir = File.dirname(__FILE__)
        Dir[File.join(dir, 'commands', '*')].each do |file|
          require file if file =~ /\.rb$/
        end
        Debugger.constants.grep(/Functions$/).map { |name| Debugger.const_get(name) }.each do |mod|
          include mod
        end
      end
      
      def method_missing(meth, *args, &block)
        if meth.to_s =~ /^(.+?)=$/
          @options[$1.intern] = args.first
        else
          if @options.has_key?(meth)
            @options[meth]
          else
            super
          end
        end
      end
      
      def options
        @options ||= {}
      end

      def unescape_incoming(str)
        str.gsub(/((?:^|[^\\])(?:\\\\)*)((?:\\n)+)/) do |_|
          $1 + "\n" * ($2.size / 2)
        end.gsub(/\\\\/, '\\')
      end

      def file_filter_supported?
        defined?(Debugger.file_filter)
      end
    end
    
    def initialize(state, printer)
      @state, @printer = state, printer
      super @printer
    end
    
    def match(input)
      @match = regexp.match(input)
    end

    protected

    def errmsg(*args)
      @printer.print_error(*args)
    end

    def print(*args)
      @state.print(*args)
    end

    # see Timeout::timeout, the difference is that we must use a DebugThread
    # because every other thread would be halted when the event hook is reached
    # in ruby-debug.c
    def timeout(sec)
      return yield if sec == nil or sec.zero?
      if Thread.respond_to?(:critical) and Thread.critical
        raise ThreadError, "timeout within critical session"      
      end
      begin
        x = Thread.current
        y = DebugThread.start {
          sleep sec
          x.raise StandardError, "Timeout: evaluation took longer than #{sec} seconds." if x.alive?
        }
        yield sec
      ensure
        y.kill if y and y.alive?
      end
    end

    def debug_eval(str, b = get_binding)
      begin
        str = str.to_s
        to_inspect = Command.unescape_incoming(str)
        max_time = Debugger.evaluation_timeout
        @printer.print_debug("Evaluating %s with timeout after %i sec", str, max_time)
        timeout(max_time) do
          eval(to_inspect, b)
        end
      rescue StandardError, ScriptError => e
        @printer.print_exception(e, @state.binding) 
        throw :debug_error
      end
    end

    def debug_silent_eval(str)
      begin
        str = str.to_s
        eval(str, get_binding)
      rescue StandardError, ScriptError
        nil
      end
    end
    
    def get_binding
      @state.context.frame_binding(@state.frame_pos)
    end

    def line_at(file, line)
      Debugger.line_at(file, line)
    end

    def get_context(thnum)
      Debugger.contexts.find{|c| c.thnum == thnum}
    end

    def realpath(filename)
      is_dir = filename.end_with?(File::SEPARATOR)
      if filename.index(File::SEPARATOR) || File::ALT_SEPARATOR && filename.index(File::ALT_SEPARATOR)
        filename = File.expand_path(filename)
      end
      if (RUBY_VERSION < '1.9') || (RbConfig::CONFIG['host_os'] =~ /mswin/)
        filename
      else
        filename = File.realpath(filename) rescue filename
        filename = filename + File::SEPARATOR if is_dir && !filename.end_with?(File::SEPARATOR)
        filename
      end
    end
  end
  
  Command.load_commands
end
