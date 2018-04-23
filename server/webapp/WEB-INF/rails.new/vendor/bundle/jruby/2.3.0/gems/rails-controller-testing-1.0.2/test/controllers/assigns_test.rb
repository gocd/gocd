require 'test_helper'

class AssignsControllerTest < ActionController::TestCase
  def test_assigns
    process :test_assigns
    # assigns can be accessed using assigns(key)
    # or assigns[key], where key is a string or
    # a symbol
    assert_equal "foo", assigns(:foo)
    assert_equal "foo", assigns("foo")
    assert_equal "foo", assigns[:foo]
    assert_equal "foo", assigns["foo"]

    # but the assigned variable should not have its own keys stringified
    expected_hash = { foo: :bar }
    assert_equal expected_hash, assigns(:foo_hash)
  end

  def test_view_assigns
    @controller = ViewAssignsController.new
    process :test_assigns
    assert_nil assigns(:foo)
    assert_nil assigns[:foo]
    assert_equal "bar", assigns(:bar)
    assert_equal "bar", assigns[:bar]
  end
end
