module RSpec
  module Core
    # @private
    module ConstMissing
      # Used to print deprecation warnings for Rspec and Spec constants (use
      # RSpec instead)
      def const_missing(name)
        case name
        when :Rspec, :Spec
          RSpec.deprecate(name.to_s, :replacement => "RSpec")
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
      RSpec.deprecate("Spec::Runner.configure", :replacement => "RSpec.configure")
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
        RSpec.deprecate("Spec::Rake::SpecTask", :replacement => "RSpec::Core::RakeTask")
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
