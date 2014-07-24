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
    
    describe 'index' do
      it "should return all associated clients" do
        expected_clients = []
        (1..5).each do
          expected_clients << create(Oauth2Provider::Client)
        end
        get :index, {use_route: :oauth_engine}
        clients = assigns[:oauth_clients]
        expect(clients.size).to eq(5)
        expect(clients.map(&:name)).to match_array(expected_clients.map(&:name))
      end
    
      it "should respond to xml route" do
        expected_clients = []
        (1..5).each do
          expected_clients << create(Oauth2Provider::Client)
        end
        get :index, {use_route: :oauth_engine, :format => :xml}
        output = Hash.from_xml(response.body)
        actual = output['oauth_clients'].map {|c| c['name']}
        expect(actual).to match_array(expected_clients.map(&:name))
      end
    end
    
    describe 'show' do
      # Might not be used by Go
    end
    
    describe 'new' do
      it "should create a new client model" do
        get :new, {use_route: :oauth_engine}
        actual = assigns[:oauth_client]
        expect(actual).to_not eq(nil)
      end
    end
    
    describe 'create' do
      it "should create a new client" do
        expected = build(Oauth2Provider::Client)
        post :create, {use_route: :oauth_client, client: {name: expected.name, redirect_uri: expected.redirect_uri}}
        actual = assigns[:oauth_client]
        expect(actual.name).to eq(expected.name)
        expect(actual.redirect_uri).to eq(expected.redirect_uri)
        expect(response).to redirect_to("/oauth2_provider/clients")
      end
      
      it "should render error when save fails" do
        post :create, {use_route: :oauth_client, client: {name: "", redirect_uri: ""}}
        error = flash.now[:error]
        expect(error).to match_array(["Redirect uri can't be blank", "Name can't be blank"])
        expect(response).to render_template("new")
      end
    end
  end
end
