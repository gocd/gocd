require 'abstract_unit'

class QueryStringParsingTest < ActionController::IntegrationTest
  class TestController < ActionController::Base
    class << self
      attr_accessor :last_query_parameters
    end

    def parse
      self.class.last_query_parameters = request.query_parameters
      head :ok
    end
  end

  def teardown
    TestController.last_query_parameters = nil
  end

  test "query string" do
    assert_parses(
      {"action" => "create_customer", "full_name" => "David Heinemeier Hansson", "customerId" => "1"},
      "action=create_customer&full_name=David%20Heinemeier%20Hansson&customerId=1"
    )
  end

  test "deep query string" do
    assert_parses(
      {'x' => {'y' => {'z' => '10'}}},
      "x[y][z]=10"
    )
  end

  test "deep query string with array" do
    assert_parses({'x' => {'y' => {'z' => ['10']}}}, 'x[y][z][]=10')
    assert_parses({'x' => {'y' => {'z' => ['10', '5']}}}, 'x[y][z][]=10&x[y][z][]=5')
  end

  test "deep query string with array of hash" do
    assert_parses({'x' => {'y' => [{'z' => '10'}]}}, 'x[y][][z]=10')
    assert_parses({'x' => {'y' => [{'z' => '10', 'w' => '10'}]}}, 'x[y][][z]=10&x[y][][w]=10')
    assert_parses({'x' => {'y' => [{'z' => '10', 'v' => {'w' => '10'}}]}}, 'x[y][][z]=10&x[y][][v][w]=10')
  end

  test "deep query string with array of hashes with one pair" do
    assert_parses({'x' => {'y' => [{'z' => '10'}, {'z' => '20'}]}}, 'x[y][][z]=10&x[y][][z]=20')
  end

  test "deep query string with array of hashes with multiple pairs" do
    assert_parses(
      {'x' => {'y' => [{'z' => '10', 'w' => 'a'}, {'z' => '20', 'w' => 'b'}]}},
      'x[y][][z]=10&x[y][][w]=a&x[y][][z]=20&x[y][][w]=b'
    )
  end

  test "query string with nil" do
    assert_parses(
      { "action" => "create_customer", "full_name" => ''},
      "action=create_customer&full_name="
    )
  end

  test "query string with array" do
    assert_parses(
      { "action" => "create_customer", "selected" => ["1", "2", "3"]},
      "action=create_customer&selected[]=1&selected[]=2&selected[]=3"
    )
  end

  test "query string with amps" do
    assert_parses(
      { "action" => "create_customer", "name" => "Don't & Does"},
      "action=create_customer&name=Don%27t+%26+Does"
    )
  end

  test "query string with many equal" do
    assert_parses(
      { "action" => "create_customer", "full_name" => "abc=def=ghi"},
      "action=create_customer&full_name=abc=def=ghi"
    )
  end

  test "query string without equal" do
    assert_parses({"action" => nil}, "action")
    assert_parses({"action" => {"foo" => nil}}, "action[foo]")
    assert_parses({"action" => {"foo" => { "bar" => nil }}}, "action[foo][bar]")
    assert_parses({"action" => {"foo" => { "bar" => nil }}}, "action[foo][bar][]")
    assert_parses({"action" => {"foo" => nil}}, "action[foo][]")
    assert_parses({"action"=>{"foo"=>[{"bar"=>nil}]}}, "action[foo][][bar]")
  end

  def test_array_parses_without_nil
    assert_parses({"action" => ['1']}, "action[]=1&action[]")
  end

  test "query string with empty key" do
    assert_parses(
      { "action" => "create_customer", "full_name" => "David Heinemeier Hansson" },
      "action=create_customer&full_name=David%20Heinemeier%20Hansson&=Save"
    )
  end

  test "query string with many ampersands" do
    assert_parses(
      { "action" => "create_customer", "full_name" => "David Heinemeier Hansson"},
      "&action=create_customer&&&full_name=David%20Heinemeier%20Hansson"
    )
  end

  test "unbalanced query string with array" do
    assert_parses(
      {'location' => ["1", "2"], 'age_group' => ["2"]},
      "location[]=1&location[]=2&age_group[]=2"
    )
  end

  private
    def assert_parses(expected, actual)
      with_routing do |set|
        set.draw do |map|
          map.connect ':action', :controller => "query_string_parsing_test/test"
        end

        get "/parse", actual
        assert_response :ok
        assert_equal(expected, TestController.last_query_parameters)
      end
    end
end
