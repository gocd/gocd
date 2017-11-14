class AssignsController < ActionController::Base
  def test_assigns
    @foo = "foo"
    @foo_hash = { foo: :bar }
    head :ok
  end
end
