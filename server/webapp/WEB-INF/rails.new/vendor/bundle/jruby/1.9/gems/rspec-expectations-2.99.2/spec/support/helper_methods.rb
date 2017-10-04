module DeprecationHelpers

  def expect_deprecation_with_call_site(file, line, snippet = //)
    expect(RSpec.configuration.reporter).to receive(:deprecation) do |options|
      matcher = include([file, line].join(':'))
      call_site = options[:call_site] || options[:message]

      unless matcher.matches?(call_site)
        # RSpec::Expectations::ExpectationNotMetError is rescued in the `match` block
        # of a custom matcher and returned as `false` from `matches?`. This would
        # prevent an expectation failure here from surfacing in the test suite if
        # it's triggered from within a `match` block, so we need to raise
        # a different error class instead.
        raise matcher.failure_message_for_should
      end

      deprecated = options[:deprecated] || options[:message]
      expect(deprecated).to match(snippet)
    end
  end

  def expect_deprecation_with_type(expression, message, type)
    expect(RSpec).to receive(:deprecate).with(expression,
      :replacement => message,
      :type        => type
    )
  end

  def expect_deprecation_with_replacement(snippet)
    expect(RSpec).to receive(:deprecate) do |_, options|
      expect(options[:replacement]).to match(snippet)
    end
  end

  def allow_deprecation
    allow(RSpec.configuration.reporter).to receive(:deprecation)
  end

  def expect_no_deprecation
    expect(RSpec.configuration.reporter).not_to receive(:deprecation)
  end
end
