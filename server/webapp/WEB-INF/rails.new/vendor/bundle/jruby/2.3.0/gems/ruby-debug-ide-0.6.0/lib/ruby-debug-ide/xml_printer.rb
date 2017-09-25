require 'stringio'
require 'cgi'
require 'monitor'

module Debugger

  class XmlPrinter # :nodoc:
    class ExceptionProxy
      instance_methods.each { |m| undef_method m unless m =~ /(^__|^send$|^object_id$|^instance_variables$|^instance_eval$)/ }

      def initialize(exception)
        @exception = exception
        @message = exception.message
        @backtrace = Debugger.cleanup_backtrace(exception.backtrace)
      end

      private 
      def method_missing(called, *args, &block) 
        @exception.__send__(called, *args, &block) 
      end
    end

    def self.protect(mname)
      return if instance_methods.include?("__#{mname}")
      alias_method "__#{mname}", mname
      class_eval %{
        def #{mname}(*args, &block)
          @@monitor.synchronize do
            return unless @interface
            __#{mname}(*args, &block)
          end
        end
      }
    end    

    @@monitor = Monitor.new
    attr_accessor :interface
    
    def initialize(interface)
      @interface = interface
    end
    
    def print_msg(*args)
      msg, *args = args
      xml_message = CGI.escapeHTML(msg % args)
      print "<message>#{xml_message}</message>"
    end

    # Sends debug message to the frontend if XML debug logging flag (--xml-debug) is on.
    def print_debug(*args)
      Debugger.print_debug(*args)
      if Debugger.xml_debug
        msg, *args = args
        xml_message = CGI.escapeHTML(msg % args)
        @interface.print("<message debug='true'>#{xml_message}</message>")
      end
    end

    def print_error(*args)
      print_element("error") do
        msg, *args = args
        print CGI.escapeHTML(msg % args)
      end
    end
    
    def print_frames(context, current_frame_id)
      print_element("frames") do
        (0...context.stack_size).each do |id|
          print_frame(context, id, current_frame_id)
        end
      end
    end
    
    def print_current_frame(frame_pos)
      print_debug "Selected frame no #{frame_pos}"
    end
    
    def print_frame(context, frame_id, current_frame_id)
      # idx + 1: one-based numbering as classic-debugger
      file = context.frame_file(frame_id)
      print "<frame no=\"%s\" file=\"%s\" line=\"%s\" #{"current='true' " if frame_id == current_frame_id}/>",
        frame_id + 1, CGI.escapeHTML(File.expand_path(file)), context.frame_line(frame_id)
    end
    
    def print_contexts(contexts)
      print_element("threads") do
        contexts.each do |c|
          print_context(c) unless c.ignored?
        end
      end
    end
    
    def print_context(context)
      print "<thread id=\"%s\" status=\"%s\" pid=\"%s\" #{current_thread_attr(context)}/>", context.thnum, context.thread.status, Process.pid
    end
    
    def print_variables(vars, kind)
      print_element("variables") do
        # print self at top position
        print_variable('self', yield('self'), kind) if vars.include?('self')
        vars.sort.each do |v|
          print_variable(v, yield(v), kind) unless v == 'self'
        end
      end
    end
    
    def print_array(array)
      print_element("variables") do
        index = 0 
        array.each { |e|
          print_variable('[' + index.to_s + ']', e, 'instance') 
          index += 1 
        }
      end
    end
    
    def print_hash(hash)
      print_element("variables") do
        hash.keys.each { | k |
          if k.class.name == "String"
            name = '\'' + k + '\''
          else
            name = k.to_s
          end
          print_variable(name, hash[k], 'instance') 
        }
      end
    end

    def print_string(string)
      print_element("variables") do
        if string.respond_to?('bytes')
          bytes = string.bytes.to_a
          InspectCommand.reference_result(bytes)
          print_variable('bytes', bytes, 'instance')
        end
        print_variable('encoding', string.encoding, 'instance') if string.respond_to?('encoding')         
      end
    end
    
    def print_variable(name, value, kind)
      name = name.to_s
      if value.nil?
        print("<variable name=\"%s\" kind=\"%s\"/>", CGI.escapeHTML(name), kind)
        return
      end
      if value.is_a?(Array) || value.is_a?(Hash)
        has_children = !value.empty?
        if has_children
          size      = value.size
          value_str = "#{value.class} (#{value.size} element#{size > 1 ? "s" : "" })"
        else
          value_str = "Empty #{value.class}"
        end
      elsif value.is_a?(String)
        has_children = value.respond_to?('bytes') || value.respond_to?('encoding')
        value_str = value
      else  
        has_children = !value.instance_variables.empty? || !value.class.class_variables.empty?
        value_str = value.to_s || 'nil' rescue "<#to_s method raised exception: #{$!}>"
        unless value_str.is_a?(String)
          value_str = "ERROR: #{value.class}.to_s method returns #{value_str.class}. Should return String." 
        end
      end

      if value_str.respond_to?('encode')
        # noinspection RubyEmptyRescueBlockInspection
        begin
         value_str = value_str.encode("UTF-8")
        rescue
        end
      end
      value_str = handle_binary_data(value_str)
      escaped_value_str = CGI.escapeHTML(value_str)
      print("<variable name=\"%s\" %s kind=\"%s\" %s type=\"%s\" hasChildren=\"%s\" objectId=\"%#+x\">",
          CGI.escapeHTML(name), build_compact_value_attr(value, value_str), kind,
          build_value_attr(escaped_value_str), value.class,
          has_children, value.respond_to?(:object_id) ? value.object_id : value.id)
      print("<value><![CDATA[%s]]></value>", escaped_value_str) if Debugger.value_as_nested_element
      print('</variable>')
    rescue StandardError => e
      print_debug "Unexpected exception \"%s\"\n%s", e.to_s, e.backtrace.join("\n")
      print("<variable name=\"%s\" kind=\"%s\" value=\"%s\"/>",
            CGI.escapeHTML(name), kind, CGI.escapeHTML(safe_to_string(value)))
    end

    def print_file_included(file)
      print("<fileIncluded file=\"%s\"/>", file)
    end

    def print_file_excluded(file)
      print("<fileExcluded file=\"%s\"/>", file)
    end

    def print_file_filter_status(status)
      print("<fileFilter status=\"%s\"/>", status)
    end

    def print_breakpoints(breakpoints)
      print_element 'breakpoints' do
        breakpoints.sort_by{|b| b.id }.each do |b|
          print "<breakpoint n=\"%d\" file=\"%s\" line=\"%s\" />", b.id, b.source, b.pos.to_s
        end
      end
    end
    
    def print_breakpoint_added(b)
      print "<breakpointAdded no=\"%s\" location=\"%s:%s\"/>", b.id, b.source, b.pos
    end
    
    def print_breakpoint_deleted(b)
      print "<breakpointDeleted no=\"%s\"/>", b.id
    end
    
    def print_breakpoint_enabled(b)
      print "<breakpointEnabled bp_id=\"%s\"/>", b.id
    end
    
    def print_breakpoint_disabled(b)
      print "<breakpointDisabled bp_id=\"%s\"/>", b.id
    end
    
    def print_contdition_set(bp_id)
      print "<conditionSet bp_id=\"%d\"/>", bp_id
    end

    def print_catchpoint_set(exception_class_name)
      print "<catchpointSet exception=\"%s\"/>", exception_class_name
    end

    def print_catchpoint_deleted(exception_class_name)
      if Debugger.catchpoint_deleted_event
        print "<catchpointDeleted exception=\"%s\"/>", exception_class_name
      else
        print_catchpoint_set(exception_class_name)
      end
    end

    def print_expressions(exps)
      print_element "expressions" do
        exps.each_with_index do |(exp, value), idx|
          print_expression(exp, value, idx+1)
        end
      end unless exps.empty?
    end
    
    def print_expression(exp, value, idx)
      print "<dispay name=\"%s\" value=\"%s\" no=\"%d\" />", exp, value, idx
    end

    def print_expression_info(incomplete, prompt, indent)
      print "<expressionInfo incomplete=\"%s\" prompt=\"%s\" indent=\"%s\"></expressionInfo>",
        incomplete, CGI.escapeHTML(prompt), indent
    end
    
    def print_eval(exp, value)
      print "<eval expression=\"%s\" value=\"%s\" />",  CGI.escapeHTML(exp), value
    end
    
    def print_pp(value)
      print value
    end
    
    def print_list(b, e, file, line)
      print "[%d, %d] in %s\n", b, e, file
      if (lines = Debugger.source_for(file))
        b.upto(e) do |n|
          if n > 0 && lines[n-1]
            if n == line
              print "=> %d  %s\n", n, lines[n-1].chomp
            else
              print "   %d  %s\n", n, lines[n-1].chomp
            end
          end
        end
      else
        print "No source-file available for %s\n", file
      end
    end
    
    def print_methods(methods)
      print_element "methods" do
        methods.each do |method|
          print "<method name=\"%s\" />", method
        end
      end
    end
    
    # Events
    
    def print_breakpoint(_, breakpoint)
      print("<breakpoint file=\"%s\" line=\"%s\" threadId=\"%d\"/>", 
      breakpoint.source, breakpoint.pos, Debugger.current_context.thnum)
    end
    
    def print_catchpoint(exception)
      context = Debugger.current_context
      print("<exception file=\"%s\" line=\"%s\" type=\"%s\" message=\"%s\" threadId=\"%d\"/>", 
      context.frame_file(0), context.frame_line(0), exception.class, CGI.escapeHTML(exception.to_s), context.thnum)
    end
    
    def print_trace(context, file, line)
      Debugger::print_debug "trace: location=\"%s:%s\", threadId=%d", file, line, context.thnum
      # TBD: do we want to clog fronend with the <trace> elements? There are tons of them.
      # print "<trace file=\"%s\" line=\"%s\" threadId=\"%d\" />", file, line, context.thnum
    end
    
    def print_at_line(context, file, line)
      print "<suspended file=\"%s\" line=\"%s\" threadId=\"%d\" frames=\"%d\"/>",
            CGI.escapeHTML(File.expand_path(file)), line, context.thnum, context.stack_size
    end
    
    def print_exception(exception, _)
      print_element("variables") do
        proxy = ExceptionProxy.new(exception)
        InspectCommand.reference_result(proxy)
        print_variable('error', proxy, 'exception')
      end
    rescue Exception
      print "<processingException type=\"%s\" message=\"%s\"/>", 
        exception.class, CGI.escapeHTML(exception.to_s)
    end
    
    def print_inspect(eval_result)
      print_element("variables") do 
        print_variable("eval_result", eval_result, 'local')
      end
    end
    
    def print_load_result(file, exception=nil)
      if exception
        print("<loadResult file=\"%s\" exceptionType=\"%s\" exceptionMessage=\"%s\"/>", file, exception.class, CGI.escapeHTML(exception.to_s))        
      else
        print("<loadResult file=\"%s\" status=\"OK\"/>", file)        
      end
    end

    def print_element(name)
      print("<#{name}>")
      begin
        yield
      ensure
        print("</#{name}>")
      end
    end

    private
    
    def print(*params)
      Debugger::print_debug(*params)
      @interface.print(*params)
    end

    def handle_binary_data(value)
      return '[Binary Data]' if (value.respond_to?('is_binary_data?') && value.is_binary_data?)
      return '[Invalid encoding]' if (value.respond_to?('valid_encoding?') && !value.valid_encoding?)
      value
    end

    def current_thread_attr(context)
      if context.thread == Thread.current
        'current="yes"'
      else
        ''
      end
    end

    def build_compact_name(value, value_str)
      return compact_array_str(value) if value.is_a?(Array)
      return compact_hash_str(value) if value.is_a?(Hash)
      return value_str[0..max_compact_name_size - 3] + '...' if value_str.size > max_compact_name_size
      nil
    rescue ::Exception => e
      print_debug(e)
      nil
    end

    def max_compact_name_size
      # todo: do we want to configure it?
      50
    end

    def compact_array_str(value)
      slice   = value[0..10]
      compact = slice.inspect
      if value.size != slice.size
        compact[0..compact.size-2] + ", ...]"
      end
      compact
    end

    def compact_hash_str(value)
      slice   = value.sort_by { |k, _| k.to_s }[0..5]
      compact = slice.map { |kv| "#{kv[0]}: #{handle_binary_data(kv[1])}" }.join(", ")
      "{" + compact + (slice.size != value.size ? ", ..." : "") + "}"
    end

    def build_compact_value_attr(value, value_str)
      compact_value_str  = build_compact_name(value, value_str)
      compact_value_str.nil? ? '' : "compactValue=\"#{CGI.escapeHTML(compact_value_str)}\""
    end

    def safe_to_string(value)
      str = value.to_s
      return str unless str.nil?

      string_io = StringIO.new
      string_io.write(value)
      string_io.string
    end

    def build_value_attr(escaped_value_str)
      Debugger.value_as_nested_element ? '' : "value=\"#{escaped_value_str}\""
    end

    instance_methods.each do |m|
      if m.to_s.index('print_') == 0
        protect m
      end
    end
    
  end

end
