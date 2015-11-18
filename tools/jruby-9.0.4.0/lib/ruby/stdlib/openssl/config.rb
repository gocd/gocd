if RUBY_VERSION > '2.2'
  load "jopenssl22/openssl/#{File.basename(__FILE__)}"
elsif RUBY_VERSION > '2.1'
  load "jopenssl21/openssl/#{File.basename(__FILE__)}"
elsif RUBY_VERSION > '1.9'
  load "jopenssl19/openssl/#{File.basename(__FILE__)}"
else
  load "jopenssl18/openssl/#{File.basename(__FILE__)}"
end