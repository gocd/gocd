module Debugger
  class CatchCommand < Command # :nodoc:
    self.control = true

    def regexp
      /^\s* cat(?:ch)? 
           (?:\s+ (\S+))? 
           (?:\s+ (off))? \s* $/ix
    end

    def execute
      excn = @match[1] 
      if not excn
        # No args given.
        errmsg "Exception class must be specified for 'catch' command"
      elsif not @match[2]
        # One arg given.
        if 'off' == excn
          Debugger.catchpoints.clear
        else
          Debugger.add_catchpoint(excn)
          print_catchpoint_set(excn)
        end
      elsif @match[2] != 'off'
        errmsg "Off expected. Got %s\n", @match[2]
      elsif Debugger.catchpoints.member?(excn)
        Debugger.catchpoints.delete(excn)
        print_catchpoint_deleted(excn)
      else
        errmsg "Catch for exception %s not found.\n", excn
      end
    end
    
    class << self
      def help_command
        'catch'
      end

      def help(cmd)
        %{
          cat[ch]\t\t\tshow catchpoint
          cat[ch] off \tremove all catch points
          cat[ch] <an Exception>\tset catchpoint to an exception
          cat[ch] <an Exception> off \tremove catchpoint for an exception
        }
      end
    end
  end
end
