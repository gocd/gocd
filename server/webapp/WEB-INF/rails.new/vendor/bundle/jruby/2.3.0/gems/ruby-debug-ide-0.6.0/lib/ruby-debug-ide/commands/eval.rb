module Debugger
  class EvalCommand < Command # :nodoc:
    #self.control = true

    def match(input)
      @input = input
      super
    end
    
    def regexp
      /^\s*(p|e(?:val)?)\s+/
    end

    def execute
      expr = @match ? @match.post_match : @input
      binding = @state.context ? get_binding : TOPLEVEL_BINDING
      print_eval expr, debug_eval(expr, binding).inspect
    end

    class << self
      def help_command
        %w|p eval|
      end

      def help(cmd)
        if cmd == 'p'
          %{
            p expression\tevaluate expression and print its value
          }
        else
          %{
            e[val] expression\tevaluate expression and print its value,
            \t\t\talias for p.
          }
        end
      end
    end
  end

  class PPCommand < Command # :nodoc:
    def regexp
      /^\s*pp\s+/
    end

    def execute
      exp = @match.post_match
      out = StringIO.new
      PP.pp(debug_eval(exp), out) rescue out.puts $!.message
      print_pp out.string
    end

    class << self
      def help_command
        'pp'
      end

      def help(cmd)
        %{
          pp expression\tevaluate expression and print its value
        }
      end
    end
  end
end
