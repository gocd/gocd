module RSpec
  module Core
    module Deprecation
      # @private
      #
      # Used internally to print deprecation warnings
      def deprecate(deprecated, data = {})
        RSpec.configuration.reporter.deprecation(
          {
            :deprecated => deprecated,
            :call_site => CallerFilter.first_non_rspec_line
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
