require 'abstract_unit'

class ViewLoadPathsTest < ActionController::TestCase
  class TestController < ActionController::Base
    def self.controller_path() "test" end
    def rescue_action(e) raise end

    before_filter :add_view_path, :only => :hello_world_at_request_time

    def hello_world() end
    def hello_world_at_request_time() render(:action => 'hello_world') end

    private
      def add_view_path
        prepend_view_path "#{FIXTURE_LOAD_PATH}/override"
      end
  end

  class Test::SubController < ActionController::Base
    layout 'test/sub'
    def hello_world; render(:template => 'test/hello_world'); end
  end

  def setup
    TestController.view_paths = nil

    @request  = ActionController::TestRequest.new
    @response = ActionController::TestResponse.new

    @controller = TestController.new
    # Following is needed in order to setup @controller.template object properly
    @controller.send :initialize_template_class, @response
    @controller.send :assign_shortcuts, @request, @response

    # Track the last warning.
    @old_behavior = ActiveSupport::Deprecation.behavior
    @last_message = nil
    ActiveSupport::Deprecation.behavior = Proc.new { |message, callback| @last_message = message }
  end

  def teardown
    ActiveSupport::Deprecation.behavior = @old_behavior
  end
  
  def test_template_load_path_was_set_correctly
    assert_equal [FIXTURE_LOAD_PATH], @controller.view_paths.map(&:to_s)
  end

  def test_controller_appends_view_path_correctly
    @controller.append_view_path 'foo'
    assert_equal [FIXTURE_LOAD_PATH, 'foo'], @controller.view_paths.map(&:to_s)

    @controller.append_view_path(%w(bar baz))
    assert_equal [FIXTURE_LOAD_PATH, 'foo', 'bar', 'baz'], @controller.view_paths.map(&:to_s)

    @controller.append_view_path(FIXTURE_LOAD_PATH)
    assert_equal [FIXTURE_LOAD_PATH, 'foo', 'bar', 'baz', FIXTURE_LOAD_PATH], @controller.view_paths.map(&:to_s)
  end

  def test_controller_prepends_view_path_correctly
    @controller.prepend_view_path 'baz'
    assert_equal ['baz', FIXTURE_LOAD_PATH], @controller.view_paths.map(&:to_s)

    @controller.prepend_view_path(%w(foo bar))
    assert_equal ['foo', 'bar', 'baz', FIXTURE_LOAD_PATH], @controller.view_paths.map(&:to_s)

    @controller.prepend_view_path(FIXTURE_LOAD_PATH)
    assert_equal [FIXTURE_LOAD_PATH, 'foo', 'bar', 'baz', FIXTURE_LOAD_PATH], @controller.view_paths.map(&:to_s)
  end

  def test_template_appends_view_path_correctly
    @controller.instance_variable_set :@template, ActionView::Base.new(TestController.view_paths, {}, @controller)
    class_view_paths = TestController.view_paths

    @controller.append_view_path 'foo'
    assert_equal [FIXTURE_LOAD_PATH, 'foo'], @controller.view_paths.map(&:to_s)

    @controller.append_view_path(%w(bar baz))
    assert_equal [FIXTURE_LOAD_PATH, 'foo', 'bar', 'baz'], @controller.view_paths.map(&:to_s)
    assert_equal class_view_paths, TestController.view_paths
  end

  def test_template_prepends_view_path_correctly
    @controller.instance_variable_set :@template, ActionView::Base.new(TestController.view_paths, {}, @controller)
    class_view_paths = TestController.view_paths

    @controller.prepend_view_path 'baz'
    assert_equal ['baz', FIXTURE_LOAD_PATH], @controller.view_paths.map(&:to_s)

    @controller.prepend_view_path(%w(foo bar))
    assert_equal ['foo', 'bar', 'baz', FIXTURE_LOAD_PATH], @controller.view_paths.map(&:to_s)
    assert_equal class_view_paths, TestController.view_paths
  end

  def test_view_paths
    get :hello_world
    assert_response :success
    assert_equal "Hello world!", @response.body
  end

  def test_view_paths_override
    TestController.prepend_view_path "#{FIXTURE_LOAD_PATH}/override"
    get :hello_world
    assert_response :success
    assert_equal "Hello overridden world!", @response.body
  end

  def test_view_paths_override_for_layouts_in_controllers_with_a_module
    @controller = Test::SubController.new
    Test::SubController.view_paths = [ "#{FIXTURE_LOAD_PATH}/override", FIXTURE_LOAD_PATH, "#{FIXTURE_LOAD_PATH}/override2" ]
    get :hello_world
    assert_response :success
    assert_equal "layout: Hello overridden world!", @response.body
  end

  def test_view_paths_override_at_request_time
    get :hello_world_at_request_time
    assert_response :success
    assert_equal "Hello overridden world!", @response.body
  end

  def test_inheritance
    original_load_paths = ActionController::Base.view_paths

    self.class.class_eval %{
      class A < ActionController::Base; end
      class B < A; end
      class C < ActionController::Base; end
    }

    A.view_paths = ['a/path']

    assert_equal ['a/path'], A.view_paths.map(&:to_s)
    assert_equal A.view_paths, B.view_paths
    assert_equal original_load_paths, C.view_paths

    C.view_paths = []
    assert_nothing_raised { C.view_paths << 'c/path' }
    assert_equal ['c/path'], C.view_paths.map(&:to_s)
  end
end
