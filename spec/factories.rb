FactoryGirl.define do
  sequence :client_name do |n|
    "client_name_#{n}"
  end
    
  factory Oauth2Provider::Client do
    name {generate(:client_name)}
    client_id SecureRandom.hex(32)
    client_secret SecureRandom.hex(32)
    redirect_uri "https://host.local:9999"
  end
end