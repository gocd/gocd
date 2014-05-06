require 'abstract_unit'

class XmlParamsParsingTest < ActionController::IntegrationTest
  class TestController < ActionController::Base
    class << self
      attr_accessor :last_request_parameters
    end

    def parse
      self.class.last_request_parameters = request.request_parameters
      head :ok
    end
  end

  def teardown
    TestController.last_request_parameters = nil
  end

  test "parses hash params" do
    with_test_routing do
      xml = "<person><name>David</name></person>"
      post "/parse", xml, default_headers
      assert_response :ok
      assert_equal({"person" => {"name" => "David"}}, TestController.last_request_parameters)
    end
  end

  test "parses single file" do
    with_test_routing do
      xml = "<person><name>David</name><avatar type='file' name='me.jpg' content_type='image/jpg'>#{ActiveSupport::Base64.encode64('ABC')}</avatar></person>"
      post "/parse", xml, default_headers
      assert_response :ok

      person = TestController.last_request_parameters
      assert_equal "image/jpg", person['person']['avatar'].content_type
      assert_equal "me.jpg", person['person']['avatar'].original_filename
      assert_equal "ABC", person['person']['avatar'].read
    end
  end

  test "logs error if parsing unsuccessful" do
    with_test_routing do
      begin
        $stderr = StringIO.new
        xml = "<person><name>David</name><avatar type='file' name='me.jpg' content_type='image/jpg'>#{ActiveSupport::Base64.encode64('ABC')}</avatar></pineapple>"
        post "/parse", xml, default_headers
        assert_response :error
        $stderr.rewind && err = $stderr.read
        assert err =~ /Error occurred while parsing request parameters/
      ensure
        $stderr = STDERR
      end
    end
  end

  test "parses multiple files" do
    xml = <<-end_body
      <person>
        <name>David</name>
        <avatars>
          <avatar type='file' name='me.jpg' content_type='image/jpg'>#{ActiveSupport::Base64.encode64('ABC')}</avatar>
          <avatar type='file' name='you.gif' content_type='image/gif'>#{ActiveSupport::Base64.encode64('DEF')}</avatar>
        </avatars>
      </person>
    end_body

    with_test_routing do
      post "/parse", xml, default_headers
      assert_response :ok
    end

    person = TestController.last_request_parameters

    assert_equal "image/jpg", person['person']['avatars']['avatar'].first.content_type
    assert_equal "me.jpg", person['person']['avatars']['avatar'].first.original_filename
    assert_equal "ABC", person['person']['avatars']['avatar'].first.read

    assert_equal "image/gif", person['person']['avatars']['avatar'].last.content_type
    assert_equal "you.gif", person['person']['avatars']['avatar'].last.original_filename
    assert_equal "DEF", person['person']['avatars']['avatar'].last.read
  end

  private
    def with_test_routing
      with_routing do |set|
        set.draw do |map|
          map.connect ':action', :controller => "xml_params_parsing_test/test"
        end
        yield
      end
    end

    def default_headers
      {'CONTENT_TYPE' => 'application/xml'}
    end
end

class LegacyXmlParamsParsingTest < XmlParamsParsingTest
  private
    def default_headers
      {'HTTP_X_POST_DATA_FORMAT' => 'xml'}
    end
end
