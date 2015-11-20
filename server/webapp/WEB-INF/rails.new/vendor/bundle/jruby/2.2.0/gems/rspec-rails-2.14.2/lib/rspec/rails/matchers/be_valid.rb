module RSpec::Rails::Matchers
  class BeValid < RSpec::Matchers::BuiltIn::Be
    def initialize(*args)
      @args = args
    end

    # @api private
    def matches?(actual)
      @actual = actual
      actual.valid?(*@args)
    end

    # @api private
    def failure_message_for_should
      message = "expected #{actual.inspect} to be valid"
      if actual.respond_to?(:errors)
        message << ", but got errors: #{actual.errors.full_messages.join(', ')}"
      end

      message
    end

    # @api private
    def failure_message_for_should_not
      "expected #{actual.inspect} not to be valid"
    end
  end

  # Passes if the given model instance's `valid?` method is true, meaning all
  # of the `ActiveModel::Validations` passed and no errors exist. If a message
  # is not given, a default message is shown listing each error.
  #
  # @example
  #
  #     thing = Thing.new
  #     thing.should be_valid
  def be_valid(*args)
    BeValid.new(*args)
  end
end
