module RSpec
  # Consistent implementation for "cleaning" the caller method to strip out
  # non-rspec lines. This enables errors to be reported at the call site in
  # the code using the library, which is far more useful than the particular
  # internal method that raised an error.

  unless defined?(CallerFilter)

    class CallerFilter

      RSPEC_LIBS = %w[
        core
        mocks
        expectations
        matchers
        rails
      ]

      ADDITIONAL_TOP_LEVEL_FILES = %w[ autorun ]

      LIB_REGEX = %r{/lib/rspec/(#{(RSPEC_LIBS + ADDITIONAL_TOP_LEVEL_FILES).join('|')})(\.rb|/)}

      if RUBY_VERSION >= '2.0.0'
        def self.first_non_rspec_line
          # `caller` is an expensive method that scales linearly with the size of
          # the stack. The performance hit for fetching it in chunks is small,
          # and since the target line is probably near the top of the stack, the
          # overall improvement of a chunked search like this is significant.
          #
          # See benchmarks/caller.rb for measurements.

          # Initial value here is mostly arbitrary, but is chosen to give good
          # performance on the common case of creating a double.
          increment = 5
          i         = 1
          line      = nil

          while !line
            stack = caller(i, increment)
            return nil unless stack

            line = stack.find { |l| l !~ LIB_REGEX }

            i         += increment
            increment *= 2 # The choice of two here is arbitrary.
          end

          line
        end
      else
        # Earlier rubies do not support the two argument form of `caller`. This
        # fallback is logically the same, but slower.
        def self.first_non_rspec_line
          caller.find { |line| line !~ LIB_REGEX }
        end
      end
    end

  end
end
