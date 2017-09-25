module Debugger
  class ThreadListCommand < Command # :nodoc:
    self.control = true
    def regexp
      /^\s*th(?:read)?\s+l(?:ist)?\s*$/
    end

    def execute
      contexts = Debugger.contexts.sort_by{|c| c.thnum}
      print_contexts(contexts)
    end

    class << self
      def help_command
        'thread'
      end

      def help(cmd)
        %{
          th[read] l[ist]\t\t\tlist all threads
        }
      end
    end
  end

  class ThreadSwitchCommand < Command # :nodoc:
    self.control = true
    self.need_context = true

    def regexp
      /^\s*th(?:read)?\s+(?:sw(?:itch)?\s+)?(\d+)\s*$/
    end

    def execute
      c = get_context(@match[1].to_i)
      case
      when c == @state.context
        print_msg "It's the current thread."
      when c.ignored?
        print_msg "Can't switch to the debugger thread."
      else
        print_context(c)
        c.stop_next = 1
        c.thread.run
        @state.proceed
      end
    end

    class << self
      def help_command
        'thread'
      end

      def help(cmd)
        %{
          th[read] [sw[itch]] <nnn>\tswitch thread context to nnn
        }
      end
    end
  end

  class ThreadInspectCommand < Command # :nodoc:
    self.control = true
    self.need_context = true

    def regexp
      /^\s*th(?:read)?\s+in(?:spect)?\s+(\d+)\s*$/
    end

    def execute
      @state.context = get_context(@match[1].to_i)      
    end

    class << self
      def help_command
        'thread'
      end

      def help(cmd)
        %{
          th[read] in[spect] <nnn>\tswitch thread context to nnn but don't resume any threads
        }
      end
    end
  end

  class ThreadStopCommand < Command # :nodoc:
    self.control = true
    self.need_context = true

    def regexp
      /^\s*th(?:read)?\s+stop\s+(\d+)\s*$/
    end

    def execute
      c = get_context(@match[1].to_i)
      case
      when c == @state.context
        print_msg "It's the current thread."
      when c.ignored?
        print_msg "Can't stop the debugger thread."
      else
        c.suspend
        print_context(c)
      end
    end

    class << self
      def help_command
        'thread'
      end

      def help(cmd)
        %{
          th[read] stop <nnn>\t\tstop thread nnn
        }
      end
    end
  end

  class ThreadCurrentCommand < Command # :nodoc:
    self.need_context = true

    def regexp
      /^\s*th(?:read)?\s+c(?:ur(?:rent)?)?\s*$/
    end

    def execute
      print_context(@state.context)
    end

    class << self
      def help_command
        'thread'
      end

      def help(cmd)
        %{
          th[read] c[ur[rent]]\t\tshow current thread
        }
      end
    end
  end

  class ThreadResumeCommand < Command # :nodoc:
    self.control = true
    self.need_context = true
    
    def regexp
      /^\s*th(?:read)?\s+resume\s+(\d+)\s*$/
    end

    def execute
      c = get_context(@match[1].to_i)
      case
      when c == @state.context
        print_msg "It's the current thread."
      when c.ignored?
        print_msg "Can't resume the debugger thread."
      else
        c.resume
        print_context(c)
      end
    end

    class << self
      def help_command
        'thread'
      end

      def help(cmd)
        %{
          th[read] resume <nnn>\t\tresume thread nnn
        }
      end
    end
  end
end
