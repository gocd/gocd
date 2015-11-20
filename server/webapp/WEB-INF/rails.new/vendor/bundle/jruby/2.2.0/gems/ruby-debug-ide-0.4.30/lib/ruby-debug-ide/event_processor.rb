require 'ruby-debug-ide/xml_printer'

 module Debugger
   
   class EventProcessor
   
     attr_accessor :line, :file, :context
    
     def initialize(interface)
       @printer = XmlPrinter.new(interface)
       @interface = interface
       @line = nil
       @file = nil
       @last_breakpoint = nil
     end
    
     def at_breakpoint(context, breakpoint)
       raise "@last_breakpoint supposed to be nil. is #{@last_breakpoint}" if @last_breakpoint
       # at_breakpoint is immediately followed by #at_line event in
       # ruby-debug-base. So postpone breakpoint printing until #at_line.
       @last_breakpoint = breakpoint
     end
     
     def at_catchpoint(context, excpt)
       @printer.print_catchpoint(excpt)
     end
     
     def at_tracing(context, file, line)
       @printer.print_trace(context, file, line)
     end
     
     def at_line(context, file, line)
       @printer.print_at_line(context, file, line) if context.nil? || context.stop_reason == :step
       line_event(context, file, line)
     end

     def at_return(context, file, line)
       @printer.print_at_line(context, file, line)
       context.stop_frame = -1
       line_event(context, file, line)
     end

     def line_event(context, file, line)
       @line = line
       @file = file
       @context = context
       if @last_breakpoint
         # followed after #at_breakpoint in the same thread. Print breakpoint
         # now when @line, @file and @context are correctly set to prevent race
         # condition with `control thread'.
         n = Debugger.breakpoints.index(@last_breakpoint) + 1
         @printer.print_breakpoint n, @last_breakpoint
         @last_breakpoint = nil
       end
       raise "DebuggerThread are not supposed to be traced (#{context.thread})" if context.thread.is_a?(Debugger::DebugThread)
       @printer.print_debug("Stopping Thread %s (%s)", context.thread.to_s, Process.pid.to_s)
       @printer.print_debug("Threads equal: %s", Thread.current == context.thread)
       IdeCommandProcessor.new(@interface).process_commands
       InspectCommand.clear_references
       @printer.print_debug("Resumed Thread %s", context.thread.to_s)
       @line = nil
       @file = nil
       @context = nil
     end

     def at_line?
        @line
     end     
    
   end
 end
