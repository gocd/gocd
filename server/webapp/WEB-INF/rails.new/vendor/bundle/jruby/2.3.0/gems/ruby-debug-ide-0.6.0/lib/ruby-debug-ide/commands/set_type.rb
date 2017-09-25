module Debugger

  # Implements debugger "set_type" command
  class SetTypeCommand < Command
    self.need_context = true

    def regexp
      / ^\s*
         set_type? \s*
         (?:\s+(\S+))?\s*
         (?:\s+(\S+))?\s*
         $
      /ix
    end

    def execute
      if RUBY_VERSION < "1.9"
        print_msg "Not implemented"
        return
      end
      begin
        expr = @match[1] + " = " + @match[2] + "(" + @match[1] + ".inspect)"
        eval(expr)
      rescue
        begin
          expr = @match[1] + " = " + @match[2] + ".new(" + @match[1] + ".inspect)"
          eval(expr)
        rescue nil
        end
      end
    end

    class << self
      def help_command
        %w[set_type]
      end

      def help(cmd)
        %{
          set_type <var> <type>

          Change the type of <var> to <type>
         }
      end
    end
  end
end
