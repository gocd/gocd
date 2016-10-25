module Debugger

  # Implements debugger "jump" command
  class JumpCommand < Command
    self.need_context = true

    def numeric?(object)
      true if Float(object) rescue false
    end

    def regexp
      / ^\s*
         j(?:ump)? \s*
         (?:\s+(\S+))?\s*
         (?:\s+(\S+))?\s*
         $
      /ix
    end

    def execute
      unless @state.context.respond_to?(:jump)
        print_msg "Not implemented"
        return
      end
      if !@match[1]
        print_msg "\"jump\" must be followed by a line number"
        return
      end
      if !numeric?(@match[1])
        print_msg "Bad line number: " + @match[1]
        return
      end
      line = @match[1].to_i
      line = @state.context.frame_line(0) + line if @match[1][0] == '+' or @match[1][0] == '-'
      if line == @state.context.frame_line(0)
        return
      end
      file = @match[2]
      file = @state.context.frame_file(file.to_i) if numeric?(file)
      file = @state.context.frame_file(0) if !file
      case @state.context.jump(line, file)
      when 0
        @state.proceed
        return
      when 1
        print_msg "Not possible to jump from here"
      when 2
        print_msg "Couldn't find debugged frame"
      when 3
        print_msg "Couldn't find active code at " + file + ":" + line.to_s
      else
        print_msg "Unknown error occurred"
      end
      @printer.print_at_line(@state.context, @state.context.frame_file, @state.context.frame_line)
    end

    class << self
      def help_command
        %w[jump]
      end

      def help(cmd)
        %{
          j[ump] line\tjump to line number (absolute)
          j[ump] -line\tjump back to line (relative)
          j[ump] +line\tjump ahead to line (relative)

          Change the next line of code to be executed.
         }
      end
    end
  end
end
