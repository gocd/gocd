module Gadgets
  module Utils
    if RUBY_PLATFORM =~ /java/
      ApacheIOUtils = org.apache.commons.io.IOUtils
      JRubyIOOutputStream = org.jruby.util.IOOutputStream
    end
    
    def copy_java_stream_to_io(java_stream, ruby_io)
      ApacheIOUtils.copy(java_stream, JRubyIOOutputStream.new(ruby_io))
    end
    
    module_function :copy_java_stream_to_io
  end
end