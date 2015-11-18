module RSpec
  module Core
    module SharedExampleGroup
      class Collection

        def initialize(sources, examples)
          @sources, @examples = sources, examples
        end

        def [](key)
          fetch_examples(key) || warn_deprecation_and_fetch_anyway(key)
        end

        private

          def fetch_examples(key)
            @examples[source_for key][key]
          end

          def source_for(key)
            @sources.reverse.find { |source| @examples[source].has_key? key }
          end

          def fetch_anyway(key)
            @examples.values.inject({}, &:merge)[key]
          end

          def warn_deprecation_and_fetch_anyway(key)
            if (example = fetch_anyway key)
              backtrace_line = caller.find { |line| !line.include?('lib/rspec/core') }
              RSpec.warn_deprecation <<-WARNING.gsub(/^ /, '')
                Accessing shared_examples defined across contexts is deprecated.
                Please declare shared_examples within a shared context, or at the top level.
                This message was generated at: #{backtrace_line}
              WARNING
              example
            end
          end

      end
    end
  end
end
