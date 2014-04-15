module RSpec
  module Core
    # @private
    module ConstMissing
      # Used to print deprecation warnings for Rspec and Spec constants (use
      # RSpec instead)
      def const_missing(name)
        case name
        when :Rspec, :Spec
          RSpec.warn_deprecation <<-WARNING
*****************************************************************
DEPRECATION WARNING: you are using a deprecated constant that will
be removed from a future version of RSpec.

#{caller(0)[2]}

* #{name} is deprecated.
* RSpec is the new top-level module in RSpec-2
*****************************************************************
WARNING
          RSpec
        else
          begin
            super
          rescue Exception => e
            e.backtrace.reject! {|l| l =~ Regexp.compile(__FILE__) }
            raise e
          end
        end
      end
    end
  end

  module Runner
    # @deprecated use RSpec.configure instead.
    def self.configure(&block)
      RSpec.deprecate("Spec::Runner.configure", "RSpec.configure")
      RSpec.configure(&block)
    end
  end

  # @private
  module Rake
    # Used to print deprecation warnings for Rake::SpecTask constant (use
    # RSpec::Core::RakeTask instead)
    def self.const_missing(name)
      case name
      when :SpecTask
        RSpec.deprecate("Spec::Rake::SpecTask", "RSpec::Core::RakeTask")
        require 'rspec/core/rake_task'
        RSpec::Core::RakeTask
      else
        begin
          super
        rescue Exception => e
          e.backtrace.reject! {|l| l =~ Regexp.compile(__FILE__) }
          raise e
        end
      end
    end

  end
end

Object.extend(RSpec::Core::ConstMissing)
