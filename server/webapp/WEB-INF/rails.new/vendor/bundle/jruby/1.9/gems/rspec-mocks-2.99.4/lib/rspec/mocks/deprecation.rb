module RSpec
  module Mocks
    module Deprecation
      # @private
      #
      # Used internally to print deprecation warnings
      def deprecate(deprecated, options={})
        message = "DEPRECATION: #{deprecated} is deprecated."
        message << " Use #{options[:replacement]} instead." if options[:replacement]
        message << " Called from #{CallerFilter.first_non_rspec_line}."
        warn message
      end

      # @private
      #
      # Used internally to print deprecation warnings
      def warn_deprecation(warning)
        message = "\nDEPRECATION: #{warning}\n"
        warn message
      end
    end
  end

  extend(Mocks::Deprecation) unless respond_to?(:deprecate)
end

