module Debugger

  class InspectCommand < Command
    # reference inspection results in order to save them from the GC
    @@references = []
    def self.reference_result(result)
      @@references << result
    end
    def self.clear_references
      @@references = []
    end
    
    def regexp
      /^\s*v(?:ar)?\s+inspect\s+/
    end
    #    
    def execute
      binding = @state.context ? get_binding : TOPLEVEL_BINDING
      obj = debug_eval(@match.post_match, binding)
      InspectCommand.reference_result(obj)
      @printer.print_inspect(obj)
    end
  end

end
