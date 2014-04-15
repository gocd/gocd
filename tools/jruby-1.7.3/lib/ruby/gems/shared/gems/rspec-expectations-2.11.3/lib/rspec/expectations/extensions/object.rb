module RSpec
  module Expectations
    module DeprecatedConstants
      # Displays deprecation warning when it captures Rspec and Spec. Otherwise
      # delegates to super.
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
***************************************************************
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

    # @deprecated (no replacement)
    def differ=(ignore)
      RSpec.deprecate("RSpec::Expectations.differ=(differ)", "nothing at all (diffing is now automatic and no longer configurable)")
    end
  end
end

extend RSpec::Expectations::DeprecatedConstants
