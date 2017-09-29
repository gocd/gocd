module RSpecHelpers
  def relative_path(path)
    RSpec::Core::Metadata.relative_path(path)
  end

  def ignoring_warnings
    original = $VERBOSE
    $VERBOSE = nil
    result = yield
    $VERBOSE = original
    result
  end

  def safely
    Thread.new do
      ignoring_warnings { $SAFE = 3 }
      yield
    end.join

    # $SAFE is not supported on Rubinius
    unless defined?(Rubinius)
      expect($SAFE).to eql 0 # $SAFE should not have changed in this thread.
    end
  end

  def expect_deprecation_with_call_site(file, line, deprecated = //)
    expect(RSpec.configuration.reporter).to receive(:deprecation) do |options|
      expect(options[:call_site]).to include([file, line].join(':'))
      expect(options[:deprecated]).to match(deprecated)
    end
  end

  def expect_deprecation_with_no_call_site(deprecated)
    expect(RSpec.configuration.reporter).to receive(:deprecation) do |options|
      expect(options).to include(:call_site => nil)
      expect(options[:deprecated]).to match(deprecated)
    end
  end

  def expect_warn_deprecation_with_call_site(file, line, deprecated = //)
    expect(RSpec.configuration.reporter).to receive(:deprecation) do |options|
      expect(options[:message]).to include([file, line].join(':'))
      expect(options[:message]).to match(deprecated)
    end
  end

  def expect_no_deprecation
    expect(RSpec.configuration.reporter).not_to receive(:deprecation)
  end

  def allow_deprecation
    allow(RSpec.configuration.reporter).to receive(:deprecation)
  end

end
