class GadgetsOauthClient < Gadgets::ModelBase
  columns :oauth_authorize_url, :client_id, :client_secret, :service_name

  validates_presence_of :service_name, :client_secret
  validates_presence_of :client_id, :message => "ID can't be empty"

  validates_uniqueness_of :service_name
  validates_uniqueness_of :oauth_authorize_url, :humanized_name => 'OAuth Authorization URL'

  validates_each :oauth_authorize_url, :key => :oauth_authorize_url_http do |record, attr, value|
    if Gadgets::Configuration.use_ssl_for_oauth
      record.errors.add(attr, 'must be a valid https url', 'OAuth Authorization URL') if value !~ Regexp.new("^https://(.+)$")
    else
      record.errors.add(attr, 'must be a valid http url', 'OAuth Authorization URL') if value !~ Regexp.new("^http://(.+)$")
    end
    unless Gadgets::Configuration.allow_localhost_authorize_url
      record.errors.add(attr, 'must not be localhost', 'OAuth Authorization URL') if value =~ Regexp.new("^http(s)?://localhost")
      record.errors.add(attr, 'must not be localhost', 'OAuth Authorization URL') if value =~ Regexp.new("^http(s)?://127.0.0.[0-9]{1,3}")
    end
  end

  def find_oauth_access_tokens_by_user_id(user_id)
    # FIXME: talking to the datasource is fugly. Just a quick fix to get tests passing.
    dto = datasource.find_oauth_access_tokens_for_client_and_user_id(self.id, user_id)
    return OauthAccessToken.new.update_from_dto(dto) if dto
  end

  def find_authorization_codes_by_user_id(user_id)
    # FIXME: talking to the datasource is fugly. Just a quick fix to get tests passing.
    dto = datasource.find_authorization_codes_for_client_and_user_id(self.id, user_id)
    return OauthAuthorizationCode.new.update_from_dto(dto) if dto
  end

  def create_oauth_access_tokens(attrs)
    OauthAccessToken.create(attrs.merge(:gadgets_oauth_client_id => self.id))
  end

  def create_oauth_authorization_codes(attrs)
    OauthAuthorizationCode.create(attrs.merge(:gadgets_oauth_client_id => self.id))
  end

  def write_attribute(attribute, value)
    value = value.strip if value.respond_to?(:strip)
    super(attribute, value)
  end

  def before_destroy
    OauthAuthorizationCode.find_all_with(:gadgets_oauth_client_id, self.id).each(&:destroy)
    OauthAccessToken.find_all_with(:gadgets_oauth_client_id, self.id).each(&:destroy)
  end

end
