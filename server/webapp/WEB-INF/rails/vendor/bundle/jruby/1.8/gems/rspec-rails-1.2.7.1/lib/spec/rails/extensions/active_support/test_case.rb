module ActiveSupport
  class TestCase
    include ::Spec::Rails::Matchers
    include ::Spec::Rails::Mocks
  end
end

