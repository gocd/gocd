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
    @client.post_content('http://example.com', { :f => 'adsf aoeu'}, {}, {"username" => "tom", "password" => "secret"})
    assert_equal('f=adsf+aoeu', RubyForge::FakeAgent.t_data)

    @client.post_content('http://example.com', { :a => 'b', :c => 'd' }, {}, {"username" => "tom", "password" => "secret"})
    assert_equal('a=b&c=d', RubyForge::FakeAgent.t_data)
  end


end
