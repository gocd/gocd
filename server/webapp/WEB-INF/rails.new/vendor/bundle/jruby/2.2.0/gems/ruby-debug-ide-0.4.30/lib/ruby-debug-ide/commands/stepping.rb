module Debugger
  class NextCommand < Command # :nodoc:
    self.need_context = true
    
    def regexp
      /^\s*n(?:ext)?([+-])?(?:\s+(\d+))?$/
    end

    def execute
      force = @match[1] == '+'
      steps = @match[2] ? @match[2].to_i : 1
      @state.context.step_over steps, @state.frame_pos, force
      @state.proceed
    end

    class << self
      def help_command
        'next'
      end

      def help(cmd)
        %{
          n[ext][+][ nnn]\tstep over once or nnn times, 
          \t\t'+' forces to move to another line
        }
      end
    end
  end

  class StepCommand < Command # :nodoc:
    self.need_context = true
    
    def regexp
      /^\s*s(?:tep)?([+-])?(?:\s+(\d+))?$/
    end

    def execute
      force = @match[1] == '+'
      steps = @match[2] ? @match[2].to_i : 1
      @state.context.step(steps, force)
      @state.proceed
    end

    class << self
      def help_command
        'step'
      end

      def help(cmd)
        %{
          s[tep][ nnn]\tstep (into methods) once or nnn times
        }
      end
    end
  end

  class FinishCommand < Command # :nodoc:
    self.need_context = true
    
    def regexp
      /^\s*fin(?:ish)?$/
    end

    def execute
      if @state.frame_pos == @state.context.stack_size - 1
        print_msg "\"finish\" not meaningful in the outermost frame."
      else
        @state.context.stop_frame = @state.frame_pos
        @state.frame_pos = 0
        @state.proceed
      end
    end

    class << self
      def help_command
        'finish'
      end

      def help(cmd)
        %{
          fin[ish]\treturn to outer frame
        }
      end
    end
  end

  class ContinueCommand < Command # :nodoc:
    def regexp
      /^\s*c(?:ont)?$/
    end

    def execute
      @state.proceed
    end

    class << self
      def help_command
        'cont'
      end

      def help(cmd)
        %{
          c[ont]\trun until program ends or hit breakpoint
        }
      end
    end
  end
end
