require File.expand_path(File.dirname(__FILE__) + '/../test_helper')

Expectations do
  expect [:c, :b, :a] do
    a = Class.new do
      include Validatable::Understandable
      understands :a
    end
    b = Class.new(a) do
      include Validatable::Understandable
      understands :b
    end
    c = Class.new(b) do
      include Validatable::Understandable
      understands :c
    end
    c.all_understandings
  end
end
