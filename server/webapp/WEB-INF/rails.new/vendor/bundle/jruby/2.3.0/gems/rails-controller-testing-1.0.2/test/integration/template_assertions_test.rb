require 'test_helper'

class TemplateAssertionsIntegrationTest < ActionDispatch::IntegrationTest
  def test_template_reset_between_requests
    get '/template_assertions/render_with_template'
    assert_template 'test/hello_world'

    get '/template_assertions/render_nothing'
    assert_template nil
  end

  def test_partial_reset_between_requests
    get '/template_assertions/render_with_partial'
    assert_template partial: 'test/_partial'

    get '/template_assertions/render_nothing'
    assert_template partial: nil
  end

  def test_layout_reset_between_requests
    get '/template_assertions/render_with_layout'
    assert_template layout: 'layouts/standard'

    get '/template_assertions/render_nothing'
    assert_template layout: nil
  end

  def test_file_reset_between_requests
    get '/template_assertions/render_file_relative_path'
    assert_template file: 'README.rdoc'

    get '/template_assertions/render_nothing'
    assert_template file: nil
  end

  def test_template_reset_between_requests_when_opening_a_session
    open_session do |session|
      session.get '/template_assertions/render_with_template'
      session.assert_template 'test/hello_world'

      session.get '/template_assertions/render_nothing'
      session.assert_template nil
    end
  end

  def test_partial_reset_between_requests_when_opening_a_session
    open_session do |session|
      session.get '/template_assertions/render_with_partial'
      session.assert_template partial: 'test/_partial'

      session.get '/template_assertions/render_nothing'
      session.assert_template partial: nil
    end
  end

  def test_layout_reset_between_requests_when_opening_a_session
    open_session do |session|
      session.get '/template_assertions/render_with_layout'
      session.assert_template layout: 'layouts/standard'

      session.get '/template_assertions/render_nothing'
      session.assert_template layout: nil
    end
  end

  def test_file_reset_between_requests_when_opening_a_session
    open_session do |session|
      session.get '/template_assertions/render_file_relative_path'
      session.assert_template file: 'README.rdoc'

      session.get '/template_assertions/render_nothing'
      session.assert_template file: nil
    end
  end

  def test_assigns_do_not_reset_template_assertion
    get '/template_assertions/render_with_layout'
    assert_equal 'hello', assigns(:variable_for_layout)
    assert_template layout: 'layouts/standard'
  end

  def test_cookies_do_not_reset_template_assertion
    get '/template_assertions/render_with_layout'
    cookies
    assert_template layout: 'layouts/standard'
  end
end
