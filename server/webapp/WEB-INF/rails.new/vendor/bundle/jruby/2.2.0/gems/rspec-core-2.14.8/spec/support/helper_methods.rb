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

  def expect_deprecation_with_call_site(file, line)
    expect(RSpec.configuration.reporter).to receive(:deprecation) do |options|
      expect(options[:call_site]).to include([file, line].join(':'))
    end
  end

  def allow_deprecation
    allow(RSpec.configuration.reporter).to receive(:deprecation)
  end

end
