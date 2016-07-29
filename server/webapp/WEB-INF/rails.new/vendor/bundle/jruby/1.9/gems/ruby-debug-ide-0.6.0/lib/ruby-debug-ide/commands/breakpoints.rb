module Debugger
  class AddBreakpoint < Command # :nodoc:
    self.control = true
    
    def regexp
      / ^\s*
        b(?:reak)?
        (?: \s+
        (?:
          (\d+) |
          (.+?)[:.#]([^.:\s]+)
        ))?
        (?:\s+
          if\s+(.+)
        )?
        $
      /x
    end

    def execute
      if @match[1]
        line, _, _, expr = @match.captures
      else
        _, file, line, expr = @match.captures
      end

      if file.nil?
        file = File.basename(@state.file)
      else
        if line !~ /^\d+$/
          klass = debug_silent_eval(file)
          if klass && !klass.kind_of?(Module)
            print_error "Unknown class #{file}"
            throw :debug_error
          end
          file = klass.name if klass
        else
          file = realpath(file)
        end
      end
      
      if line =~ /^\d+$/
        line = line.to_i
      else
        line = line.intern.id2name
      end
      
      b = Debugger.add_breakpoint file, line, expr
      print_breakpoint_added b
    end

    class << self
      def help_command
        'break'
      end

      def help(cmd)
        %{
          b[reak] file:line [if expr]
          b[reak] [file|class(:|.|#)]<line|method> [if expr] -
          \tset breakpoint to some position, (optionally) if expr == true
        }
      end
    end
  end

  class BreakpointsCommand < Command # :nodoc:
    self.control = true

    def regexp
      /^\s*info\s*break$/
    end

    def execute
      print_breakpoints Debugger.breakpoints
    end

    class << self
      def help_command
        'break'
      end

      def help(cmd)
        %{
          b[reak]\tlist breakpoints
        }
      end
    end
  end

  class DeleteBreakpointCommand < Command # :nodoc:
    self.control = true

    def regexp
      /^\s*del(?:ete)?(?:\s+(.*))?$/
    end

    def execute
      brkpts = @match[1]
      unless brkpts
          Debugger.breakpoints.clear
      else
        brkpts.split(/[ \t]+/).each do |pos|
          pos = get_int(pos, "Delete", 1)
          return unless pos
          b = Debugger.remove_breakpoint(pos)
          if b
            print_breakpoint_deleted b
          else
            print_error "No breakpoint number %d\n", pos
          end
        end
      end
    end

    class << self
      def help_command
        'delete'
      end

      def help(cmd)
        %{
          del[ete][ nnn...]\tdelete some or all breakpoints
        }
      end
    end
  end
end
