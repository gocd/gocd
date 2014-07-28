class CreateTables < ActiveRecord::Migration
  def change
    create_table :oauthclients, :force => true do |t|
      t.string :name
      t.string :clientid
      t.string :clientsecret
      t.string :redirecturi
    end
    create_table :oauthauthorizations, :force => true do |t|
      t.string :userid
      t.string :oauthclientid
      t.string :code
      t.string :expiresat
    end
    create_table :oauthtokens, :force => true do |t|
      t.string :userid
      t.string :oauthclientid
      t.string :accesstoken
      t.string :refreshtoken
      t.string :expiresat
    end
    add_index :oauthclients, [:name], :unique => true
  end
end