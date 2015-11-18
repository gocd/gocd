module RSpec::Rails::Matchers
  class BeANew < RSpec::Matchers::BuiltIn::BaseMatcher

    def initialize(expected)
      @expected = expected
    end

    # @api private
    def matches?(actual)
      @actual = actual
      actual.is_a?(expected) && actual.new_record? && attributes_match?(actual)
    end

    # Use this to specify the specific attributes to match on the new record.
    #
    # @example
    #
    #     it "assigns a new Thing with the submitted attributes" do
    #       post :create, :thing => { :name => "Illegal Value" }
    #       assigns(:thing).should be_a_new(Thing).with(:name => nil)
    #     end
    def with(expected_attributes)
      attributes.merge!(expected_attributes)
      self
    end

    # @api private
    def failure_message_for_should
      [].tap do |message|
        unless actual.is_a?(expected) && actual.new_record?
          message << "expected #{actual.inspect} to be a new #{expected.inspect}"
        end
        unless attributes_match?(actual)
          if unmatched_attributes.size > 1
            message << "attributes #{unmatched_attributes.inspect} were not set on #{actual.inspect}"
          else
            message << "attribute #{unmatched_attributes.inspect} was not set on #{actual.inspect}"
          end
        end
      end.join(' and ')
    end

    private

    def attributes
      @attributes ||= {}
    end

    def attributes_match?(actual)
      attributes.stringify_keys.all? do |key, value|
        actual.attributes[key].eql?(value)
      end
    end

    def unmatched_attributes
      attributes.stringify_keys.reject do |key, value|
        actual.attributes[key].eql?(value)
      end
    end
  end

  # Passes if actual is an instance of `model_class` and returns `false` for
  # `persisted?`. Typically used to specify instance variables assigned to
  # views by controller actions
  #
  # @example
  #
  #     get :new
  #     assigns(:thing).should be_a_new(Thing)
  #
  #     post :create, :thing => { :name => "Illegal Value" }
  #     assigns(:thing).should be_a_new(Thing).with(:name => nil)
  def be_a_new(model_class)
    BeANew.new(model_class)
  end
end
