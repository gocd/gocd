module Debugger
  class LoadCommand < Command  
    def regexp
      /^\s*load\s+/
    end
    
    def execute
      fileName = @match.post_match
      @printer.print_debug("loading file: %s", fileName)
      begin
        load fileName
        @printer.print_load_result(fileName)
      rescue Exception => error
        @printer.print_load_result(fileName, error)
      end
    end
  end
end