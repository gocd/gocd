class ViewAssignsController < ActionController::Base
  def test_assigns
    @foo = "foo"
    head :ok
  end

  def view_assigns
    { "bar" => "bar" }
  end
end
