if RUBY_VERSION >= '2.1.0'
  load('jopenssl21/openssl/config.rb')
elsif RUBY_VERSION >= '1.9.0'
  load('jopenssl19/openssl/config.rb')
else
  load('jopenssl18/openssl/config.rb')
end
