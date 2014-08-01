class OauthAuthorizationCode < Gadgets::ModelBase
  self.db_columns = self.db_columns.dup
  columns :user_id, :code, :expires_in => :integer, :gadgets_oauth_client_id => :integer

  validates_uniqueness_of :user_id, :scope => :gadgets_oauth_client_id
  validates_presence_of :code, :user_id, :gadgets_oauth_client_id

end
