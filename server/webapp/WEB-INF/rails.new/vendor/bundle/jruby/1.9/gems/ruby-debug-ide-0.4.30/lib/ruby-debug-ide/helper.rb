module Debugger

  module ParseFunctions
    # Parse 'str' of command 'cmd' as an integer between
    # min and max. If either min or max is nil, that
    # value has no bound.
    def get_int(str, cmd, min=nil, max=nil, default=1)
      return default unless str
      begin
        int = Integer(str)
        if min and int < min
          print_error "%s argument '%s' needs to at least %s.\n" % [cmd, str, min]
          return nil
        elsif max and int > max
          print_error "%s argument '%s' needs to at most %s.\n" % [cmd, str, max]
          return nil
        end
        return int
      rescue
        print_error "%s argument '%s' needs to be a number.\n" % [cmd, str]
        return nil
      end
    end

    # Return true if code is syntactically correct for Ruby.
    def syntax_valid?(code)
      eval("BEGIN {return true}\n#{code}", nil, "", 0)
    rescue Exception
      false
    end 

  end
end
