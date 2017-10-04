module RSpec
  module Expectations
    module DeprecatedConstants
      # Displays deprecation warning when it captures Rspec and Spec. Otherwise
      # delegates to super.
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

    # @deprecated (no replacement)
    def differ=(ignore)
      RSpec.deprecate("RSpec::Expectations.differ=(differ)")
    end
  end
end

extend RSpec::Expectations::DeprecatedConstants
