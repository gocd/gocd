module RSpec::Rails::Matchers
  class BeANewRecord < RSpec::Matchers::BuiltIn::BaseMatcher

    # @api private
    def matches?(actual)
      !actual.persisted?
    end

    def failure_message_for_should
      "expected #{actual.inspect} to be a new record, but was persisted"
    end

    def failure_message_for_should_not
      "expected #{actual.inspect} to be persisted, but was a new record"
    end
  end

  # Passes if actual returns `false` for `persisted?`.
  #
  # @example
  #
  #     get :new
  #     assigns(:thing).should be_new_record
  def be_new_record
    BeANewRecord.new
  end
end
