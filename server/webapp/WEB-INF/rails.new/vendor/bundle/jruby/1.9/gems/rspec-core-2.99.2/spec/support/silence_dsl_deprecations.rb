module SilenceDSLDeprecations
  class Reporter
    def initialize(reporter)
      @reporter = reporter
    end

    def deprecation(*)
      # do nothing
    end

    def respond_to?(*args)
      @reporter.respond_to?(*args) || super
    end

    def method_missing(*args, &block)
      @reporter.__send__(*args, &block)
    end
  end

  def silence_dsl_deprecations
    old_reporter = RSpec.configuration.reporter
    replace_reporter(Reporter.new(old_reporter))
    yield
  ensure
    replace_reporter(old_reporter)
  end

  def replace_reporter(new_reporter)
    RSpec.configuration.instance_variable_set(:@reporter, new_reporter)
  end
end

