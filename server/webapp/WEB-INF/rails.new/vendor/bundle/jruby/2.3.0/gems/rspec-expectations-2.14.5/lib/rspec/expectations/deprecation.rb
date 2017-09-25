module RSpec
  module Expectations
    module Deprecation
     RSPEC_LIBS = %w[
        core
        mocks
        expectations
        matchers
        rails
      ]

      ADDITIONAL_TOP_LEVEL_FILES = %w[ autorun ]

      LIB_REGEX = %r{/lib/rspec/(#{(RSPEC_LIBS + ADDITIONAL_TOP_LEVEL_FILES).join('|')})(\.rb|/)}

      # @private
      #
      # Used internally to print deprecation warnings
      def deprecate(deprecated, options={})
        call_site = caller.find { |line| line !~ LIB_REGEX }

        message = "DEPRECATION: #{deprecated} is deprecated."
        message << " Use #{options[:replacement]} instead." if options[:replacement]
        message << " Called from #{call_site}."
        warn message
      end
    end
  end

  extend(Expectations::Deprecation) unless respond_to?(:deprecate)
end
