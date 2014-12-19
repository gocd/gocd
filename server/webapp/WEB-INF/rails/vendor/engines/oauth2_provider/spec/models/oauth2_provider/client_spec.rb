require File.expand_path(File.join(File.dirname(__FILE__), "..", "..", "spec_helper"))

module Oauth2Provider
  describe Oauth2Provider::Client do
    it "should validate redirect_uri format" do
      c = Client.new({name: 'foo', client_id: 'cid', client_secret: 'cs', redirect_uri: 'foobar'})
      result = c.save
      expect(result).to eq(false)
      expect(c.errors).to have_key(:redirect_uri)
    end
    
    it "should validate presence of name" do
      c = Client.new({name: '', client_id: 'cid', client_secret: 'cs', redirect_uri: 'https://foo'})
      result = c.save
      expect(result).to eq(false)
      expect(c.errors).to have_key(:name)
    end
    
    it "should validate presence of redirect_uri" do
      c = Client.new({name: 'foo', client_id: 'cid', client_secret: 'cs', redirect_uri: ''})
      result = c.save
      expect(result).to eq(false)
      expect(c.errors).to have_key(:redirect_uri)
    end
    
    it "should validate unique name" do
      client = create(Oauth2Provider::Client)
      new_client = build(Oauth2Provider::Client, name: client.name)
      result = new_client.save
      expect(result).to eq(false)
      expect(new_client.errors).to have_key(:name)
    end
    
    it "should not validate unique name when existing client is edited" do
      client = create(Oauth2Provider::Client)
      client.update_attributes({name: client.name, redirect_uri: "http://new.url"})
      client.reload
      expect(client.redirect_uri).to eq("http://new.url")
    end
  end
end