require 'test_helper'

<% module_namespacing do -%>
class <%= controller_class_name %>ControllerTest < ActionDispatch::IntegrationTest
  <%- if mountable_engine? -%>
  include Engine.routes.url_helpers

  <%- end -%>
  setup do
    @<%= singular_table_name %> = <%= fixture_name %>(:one)
  end

  test "should get index" do
    get <%= index_helper %>_url
    assert_response :success
  end

  test "should get new" do
    get <%= new_helper %>
    assert_response :success
  end

  test "should create <%= singular_table_name %>" do
    assert_difference('<%= class_name %>.count') do
      post <%= index_helper %>_url, params: { <%= "#{singular_table_name}: { #{attributes_hash} }" %> }
    end

    assert_redirected_to <%= singular_table_name %>_url(<%= class_name %>.last)
  end

  test "should show <%= singular_table_name %>" do
    get <%= show_helper %>
    assert_response :success
  end

  test "should get edit" do
    get <%= edit_helper %>
    assert_response :success
  end

  test "should update <%= singular_table_name %>" do
    patch <%= show_helper %>, params: { <%= "#{singular_table_name}: { #{attributes_hash} }" %> }
    assert_redirected_to <%= singular_table_name %>_url(<%= "@#{singular_table_name}" %>)
  end

  test "should destroy <%= singular_table_name %>" do
    assert_difference('<%= class_name %>.count', -1) do
      delete <%= show_helper %>
    end

    assert_redirected_to <%= index_helper %>_url
  end
end
<% end -%>
