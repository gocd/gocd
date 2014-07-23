require File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "spec_helper"))

module Oauth2Provider
  describe Oauth2Provider::ClientsController do
    before :each do
      module Oauth2Provider::SslHelper
        def mandatory_ssl
          true
        end
      end
    end
    
    it "should return all associated clients" do
      expected_clients = []
      (1..5).each do
        expected_clients << create(Oauth2Provider::Client)
      end
      get 'index', { use_route: :oauth_engine }
      clients = assigns[:oauth_clients]
      expect(clients.size).to eq(5)
      expect(clients.map(&:name)).to match_array(expected_clients.map(&:name))
    end
    
    it "should respond to xml route" do
      expected_clients = []
      (1..5).each do
        expected_clients << create(Oauth2Provider::Client)
      end
      get 'index', { use_route: :oauth_engine, :format => :xml }
      output = Hash.from_xml(response.body)
      actual = output['oauth_clients'].map {|c| c['name']}
      expect(actual).to match_array(expected_clients.map(&:name))
    end
  end
end
