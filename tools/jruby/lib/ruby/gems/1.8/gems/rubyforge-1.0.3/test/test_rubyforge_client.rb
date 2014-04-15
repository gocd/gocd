require 'test/unit' unless defined? $ZENTEST and $ZENTEST
require 'rubyforge'

class RubyForge::FakeAgent
  class << self
    attr_accessor :t_data, :t_request
  end

  def initialize(*args)
  end

  def request(request, data)
    self.class.t_request = request
    self.class.t_data = data
    response = Net::HTTPOK.new('1.1', 200, '')
    def response.read_body; ''; end
    return response
  end

  class Post
    def initialize(*args)
      @args = args
      @stuff = {}
    end

    def [] key
      @stuff[key.downcase]
    end

    def []= key, val
      @stuff[key.downcase] = val
    end

    def method_missing(*stuff)
      # warn stuff.inspect
    end
  end
end

class TestRubyForgeClient < Test::Unit::TestCase
  def setup
    @client                        = RubyForge::Client.new
    @client.agent_class            = RubyForge::FakeAgent
    RubyForge::FakeAgent.t_data    = :unassigned
    RubyForge::FakeAgent.t_request = :unassigned
  end

  def test_post_with_params
    @client.post_content('http://example.com', { :f => 'adsf aoeu'})
    assert_equal('f=adsf+aoeu', RubyForge::FakeAgent.t_data)

    @client.post_content('http://example.com', { :a => 'b', :c => 'd' })
    assert_equal('a=b&c=d', RubyForge::FakeAgent.t_data)
  end

  def test_multipart_post_one_param
    random = Array::new(8){ "%2.2d" % rand(42) }.join('__')
    boundary = "multipart/form-data; boundary=___#{ random }___"

    expected = <<-END
--___#{random}___\r
Content-Disposition: form-data; name="a"\r\n\r
a b c\r
--___#{random}___--\r
END

    @client.post_content( 'http://example.com',
                          { :a => 'a b c' },
                          { 'content-type' => boundary }
                        )
    assert_equal(expected, RubyForge::FakeAgent.t_data)
  end

  def test_multipart_post_two_params
    random = Array::new(8){ "%2.2d" % rand(42) }.join('__')
    boundary = "multipart/form-data; boundary=___#{ random }___"

    request = <<-END
--___#{random}___\r
Content-Disposition: form-data; name="a"\r\n\r
b\r
--___#{random}___\r
Content-Disposition: form-data; name="c"\r\n\r
d\r
--___#{random}___--\r
END

    @client.post_content( 'http://example.com',
                          { :a => 'b', :c => 'd' },
                          { 'content-type' => boundary }
                        )
    assert_equal(request, RubyForge::FakeAgent.t_data)
  end

  def test_multipart_io
    random = Array::new(8){ "%2.2d" % rand(42) }.join('__')
    boundary = "multipart/form-data; boundary=___#{ random }___"

    file_contents = 'blah blah blah'
    file = StringIO.new(file_contents)
    class << file
      def path
        '/one/two/three.rb'
      end
    end

    request = <<-END
--___#{random}___\r
Content-Disposition: form-data; name="userfile"; filename="three.rb"\r
Content-Transfer-Encoding: binary\r
Content-Type: text/plain\r\n\r
#{file_contents}\r
--___#{random}___--\r
END

    @client.post_content( 'http://example.com',
                          { :userfile => file },
                          { 'content-type' => boundary }
                        )
    assert_equal(request, RubyForge::FakeAgent.t_data)
  end
end
