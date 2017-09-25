module Debugger
  class IncludeFile < Command # :nodoc:
    self.control = true

    def regexp
      / ^\s*include\s+(.+?)\s*$/x
    end

    def execute
      file = @match[1]

      return if file.nil?
      file = realpath(file)

      if Command.file_filter_supported?
        Debugger.file_filter.include(file)
        print_file_included(file)
      else
        print_debug("file filter is not supported")
      end
    end

    class << self
      def help_command
        'include'
      end

      def help(cmd)
        %{
          include file - adds file/dir to file filter (either remove already excluded or add as included)
        }
      end
    end
  end

  class ExcludeFile < Command # :nodoc:
    self.control = true

    def regexp
      / ^\s*exclude\s+(.+?)\s*$/x
    end

    def execute
      file = @match[1]

      return if file.nil?
      file = realpath(file)

      if Command.file_filter_supported?
        Debugger.file_filter.exclude(file)
        print_file_excluded(file)
      else
        print_debug("file filter is not supported")
      end
    end

    class << self
      def help_command
        'include'
      end

      def help(cmd)
        %{
          exclude file - exclude file/dir from file filter (either remove already included or add as exclude)
        }
      end
    end
  end

  class FileFilterCommand < Command # :nodoc:
    self.control = true

    def regexp
      / ^\s*file-filter\s+(on|off)\s*$/x
    end

    def execute
      action = @match[1]

      if Command.file_filter_supported?
        if 'on' == action
          Debugger.file_filter.enable
          print_file_filter_status(true)
        elsif 'off' == action
          Debugger.file_filter.disable
          print_file_filter_status(false)
        else
          print_error "Unknown option '#{action}'"
        end
      else
        print_debug("file filter is not supported")
      end
    end

    class << self
      def help_command
        'file-filter'
      end

      def help(cmd)
        %{
          file-filter (on|off) - enable/disable file  filtering
        }
      end
    end
  end
end