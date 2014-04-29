module RSpec
  module Core
    module Deprecation
      # @private
      #
      # Used internally to print deprecation warnings
      def deprecate(deprecated, replacement_or_hash={}, ignore_version=nil)
        # Temporarily support old and new APIs while we transition the other
        # rspec libs to use a hash for the 2nd arg and no version arg
        data = Hash === replacement_or_hash ? replacement_or_hash : { :replacement => replacement_or_hash }
        call_site = caller.find { |line| line !~ %r{/lib/rspec/(core|mocks|expectations|matchers|rails)/} }

        RSpec.configuration.reporter.deprecation(
          {
            :deprecated => deprecated,
            :call_site => call_site
          }.merge(data)
        )
      end

      # @private
      #
      # Used internally to print deprecation warnings
      def warn_deprecation(message)
        RSpec.configuration.reporter.deprecation :message => message
      end
    end
  end

  extend(Core::Deprecation)
end
