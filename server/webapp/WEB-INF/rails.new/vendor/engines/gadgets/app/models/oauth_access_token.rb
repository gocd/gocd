class OauthAccessToken < Gadgets::ModelBase
  columns :user_id, :access_token, :refresh_token, :gadgets_oauth_client_id => :integer, :expires_in => :integer
  
  validates_uniqueness_of :user_id, :scope => :gadgets_oauth_client_id
  validates_presence_of :access_token, :user_id, :gadgets_oauth_client_id

end
