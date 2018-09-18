
require 'action_view/test_case'

module NoStrongParams
  def initialize
    super
    @params = @params.permit!.to_unsafe_h
  end
end


ActionView::TestCase::TestController.send(:prepend, NoStrongParams)
