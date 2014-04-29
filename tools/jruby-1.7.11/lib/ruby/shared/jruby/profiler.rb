
require 'java'

module JRuby
  module Profiler
    java_import org.jruby.runtime.profile.ProfilePrinter
    java_import org.jruby.runtime.profile.FlatProfilePrinter
    java_import org.jruby.runtime.profile.GraphProfilePrinter
    java_import org.jruby.runtime.profile.HtmlProfilePrinter
    java_import org.jruby.runtime.profile.JsonProfilePrinter
    
    def self.profile(&block)
      start
      profiled_code(&block)
      stop
    end
    
    def self.profiled_code
      yield
    end
    
    def self.clear
      profile_data.clear
    end
    
    protected

      def self.start
        current_thread_context.start_profiling
        clear
      end

      def self.stop
        current_thread_context.stop_profiling
        profile_data
      end

      def self.profile_data
        current_thread_context.profile_data
      end
    
    private
    
      def self.runtime
        JRuby.runtime
      end

      def self.current_thread_context
        runtime.get_thread_service.get_current_context
      end
      
  end
end
