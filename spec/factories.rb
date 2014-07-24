FactoryGirl.define do
  sequence :client_name do |n|
    "client_name_#{n}"
  end
  
  sequence :user_id do |n|
    n
  end
    
  factory Oauth2Provider::Client do
    name {generate(:client_name)}
    client_id SecureRandom.hex(32)
    client_secret SecureRandom.hex(32)
    redirect_uri "https://host.local:9999"
  end
  
  factory Oauth2Provider::Authorization do
    user_id {generate(:user_id)}
    client_id {client = create(Oauth2Provider::Client); client.id}
    code SecureRandom.hex(32)
    expires_at (Time.now + 1.hour).to_i
  end
  
  factory Oauth2Provider::Token do
    user_id {generate(:user_id)}
    client_id {client = create(Oauth2Provider::Client); client.id}
    access_token SecureRandom.hex(32)
    refresh_token SecureRandom.hex(32)
    expires_at (Time.now + 1.hour).to_i
  end
end