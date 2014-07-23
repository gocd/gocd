class CreateTables < ActiveRecord::Migration
  def change
    create_table :oauth_clients, :force => true do |t|
      t.string :name
      t.string :clientid
      t.string :clientsecret
      t.string :redirecturi
    end
  end
end