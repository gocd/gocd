# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

class CreateOauthClients < ActiveRecord::Migration
  def self.up
    create_table :oauth_clients do |t|
      t.string :name
      t.string :client_id
      t.string :client_secret
      t.string :redirect_uri

      t.timestamps
    end
  end

  def self.down
    drop_table :oauth_clients
  end
end
