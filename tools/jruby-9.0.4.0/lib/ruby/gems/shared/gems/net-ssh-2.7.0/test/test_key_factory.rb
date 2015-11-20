require 'common'
require 'net/ssh/key_factory'

class TestKeyFactory < Test::Unit::TestCase
  def setup
    @key_file = File.expand_path("/key-file")
  end
  
  def test_load_unencrypted_private_RSA_key_should_return_key
    File.expects(:read).with(@key_file).returns(rsa_key.export)
    assert_equal rsa_key.to_der, Net::SSH::KeyFactory.load_private_key(@key_file).to_der
  end

  def test_load_unencrypted_private_DSA_key_should_return_key
    File.expects(:read).with(@key_file).returns(dsa_key.export)
    assert_equal dsa_key.to_der, Net::SSH::KeyFactory.load_private_key(@key_file).to_der
  end

  def test_load_encrypted_private_RSA_key_should_prompt_for_password_and_return_key
    File.expects(:read).with(@key_file).returns(encrypted(rsa_key, "password"))
    Net::SSH::KeyFactory.expects(:prompt).with("Enter passphrase for #{@key_file}:", false).returns("password")
    assert_equal rsa_key.to_der, Net::SSH::KeyFactory.load_private_key(@key_file).to_der
  end

  def test_load_encrypted_private_RSA_key_with_password_should_not_prompt_and_return_key
    File.expects(:read).with(@key_file).returns(encrypted(rsa_key, "password"))
    assert_equal rsa_key.to_der, Net::SSH::KeyFactory.load_private_key(@key_file, "password").to_der
  end

  def test_load_encrypted_private_DSA_key_should_prompt_for_password_and_return_key
    File.expects(:read).with(@key_file).returns(encrypted(dsa_key, "password"))
    Net::SSH::KeyFactory.expects(:prompt).with("Enter passphrase for #{@key_file}:", false).returns("password")
    assert_equal dsa_key.to_der, Net::SSH::KeyFactory.load_private_key(@key_file).to_der
  end

  def test_load_encrypted_private_DSA_key_with_password_should_not_prompt_and_return_key
    File.expects(:read).with(@key_file).returns(encrypted(dsa_key, "password"))
    assert_equal dsa_key.to_der, Net::SSH::KeyFactory.load_private_key(@key_file, "password").to_der
  end

  def test_load_encrypted_private_key_should_give_three_tries_for_the_password_and_then_raise_exception
    File.expects(:read).with(@key_file).returns(encrypted(rsa_key, "password"))
    Net::SSH::KeyFactory.expects(:prompt).times(3).with("Enter passphrase for #{@key_file}:", false).returns("passwod","passphrase","passwd")
    if OpenSSL::PKey.respond_to?(:read)
      error_class = ArgumentError
    else
      error_class = OpenSSL::PKey::RSAError
    end
    assert_raises(error_class) { Net::SSH::KeyFactory.load_private_key(@key_file) }
  end

  def test_load_encrypted_private_key_should_raise_exception_without_asking_passphrase
    File.expects(:read).with(@key_file).returns(encrypted(rsa_key, "password"))
    Net::SSH::KeyFactory.expects(:prompt).never
    if OpenSSL::PKey.respond_to?(:read)
      error_class = ArgumentError
    else
      error_class = OpenSSL::PKey::RSAError
    end
    assert_raises(error_class) { Net::SSH::KeyFactory.load_private_key(@key_file, nil, false) }
  end

  def test_load_public_rsa_key_should_return_key
    File.expects(:read).with(@key_file).returns(public(rsa_key))
    assert_equal rsa_key.to_blob, Net::SSH::KeyFactory.load_public_key(@key_file).to_blob
  end

  if defined?(OpenSSL::PKey::EC)
    def test_load_unencrypted_private_ecdsa_sha2_nistp256_key_should_return_key
      File.expects(:read).with("/key-file").returns(ecdsa_sha2_nistp256_key.to_pem)
      assert_equal ecdsa_sha2_nistp256_key.to_der, Net::SSH::KeyFactory.load_private_key("/key-file").to_der
    end
    def test_load_unencrypted_private_ecdsa_sha2_nistp384_key_should_return_key
      File.expects(:read).with("/key-file").returns(ecdsa_sha2_nistp384_key.to_pem)
      assert_equal ecdsa_sha2_nistp384_key.to_der, Net::SSH::KeyFactory.load_private_key("/key-file").to_der
    end
    def test_load_unencrypted_private_ecdsa_sha2_nistp521_key_should_return_key
      File.expects(:read).with("/key-file").returns(ecdsa_sha2_nistp521_key.to_pem)
      assert_equal ecdsa_sha2_nistp521_key.to_der, Net::SSH::KeyFactory.load_private_key("/key-file").to_der
    end

    def test_load_public_ecdsa_sha2_nistp256_key_should_return_key
      File.expects(:read).with("/key-file").returns(public(ecdsa_sha2_nistp256_key))
      assert_equal ecdsa_sha2_nistp256_key.to_blob, Net::SSH::KeyFactory.load_public_key("/key-file").to_blob
    end
    def test_load_public_ecdsa_sha2_nistp384_key_should_return_key
      File.expects(:read).with("/key-file").returns(public(ecdsa_sha2_nistp384_key))
      assert_equal ecdsa_sha2_nistp384_key.to_blob, Net::SSH::KeyFactory.load_public_key("/key-file").to_blob
    end
    def test_load_public_ecdsa_sha2_nistp521_key_should_return_key
      File.expects(:read).with("/key-file").returns(public(ecdsa_sha2_nistp521_key))
      assert_equal ecdsa_sha2_nistp521_key.to_blob, Net::SSH::KeyFactory.load_public_key("/key-file").to_blob
    end
  end

  def test_load_anonymous_private_key_should_return_key_or_raise_exception
    File.expects(:read).with(@key_file).returns(anonymous_private_key)
    if OpenSSL::PKey.respond_to?(:read)
      assert_equal OpenSSL::PKey::RSA.new(anonymous_private_key).to_der, Net::SSH::KeyFactory.load_private_key(@key_file).to_der
    else
      assert_raises(OpenSSL::PKey::PKeyError) { Net::SSH::KeyFactory.load_private_key(@key_file) }
    end
  end

  private

    def rsa_key
      # 512 bits
      @rsa_key ||= OpenSSL::PKey::RSA.new("0\202\001;\002\001\000\002A\000\235\236\374N\e@2E\321\3757\003\354c\276N\f\003\3479Ko\005\317\0027\a\255=\345!\306\220\340\211;\027u\331\260\362\2063x\332\301y4\353\v%\032\214v\312\304\212\271GJ\353\2701\031\002\003\001\000\001\002@\022Y\306*\031\306\031\224Cde\231QV3{\306\256U\2477\377\017\000\020\323\363R\332\027\351\034\224OU\020\227H|pUS\n\263+%\304\341\321\273/\271\e\004L\250\273\020&,\t\304By\002!\000\311c\246%a\002\305\277\262R\266\244\250\025V_\351]\264\016\265\341\355\305\223\347Z$8\205#\023\002!\000\310\\\367|\243I\363\350\020\307\246\302\365\ed\212L\273\2158M\223w\a\367 C\t\224A4\243\002!\000\262]+}\327\231\331\002\2331^\312\036\204'g\363\f&\271\020\245\365-\024}\306\374e\202\2459\002 }\231\341\276\3551\277\307{5\\\361\233\353G\024wS\237\fk}\004\302&\205\277\340rb\211\327\002!\000\223\307\025I:\215_\260\370\252\3757\256Y&X\364\354\342\215\350\203E8\227|\f\237M\375D|")
    end

    def dsa_key
      # 512 bits
      @dsa_key ||= OpenSSL::PKey::DSA.new("0\201\367\002\001\000\002A\000\203\316/\037u\272&J\265\003l3\315d\324h\372{\t8\252#\331_\026\006\035\270\266\255\343\353Z\302\276\335\336\306\220\375\202L\244\244J\206>\346\b\315\211\302L\246x\247u\a\376\366\345\302\016#\002\025\000\244\274\302\221Og\275/\302+\356\346\360\024\373wI\2573\361\002@\027\215\270r*\f\213\350C\245\021:\350 \006\\\376\345\022`\210b\262\3643\023XLKS\320\370\002\276\347A\nU\204\276\324\256`=\026\240\330\306J\316V\213\024\e\030\215\355\006\037q\337\356ln\002@\017\257\034\f\260\333'S\271#\237\230E\321\312\027\021\226\331\251Vj\220\305\316\036\v\266+\000\230\270\177B\003?t\a\305]e\344\261\334\023\253\323\251\223M\2175)a(\004\"lI8\312\303\307\a\002\024_\aznW\345\343\203V\326\246ua\203\376\201o\350\302\002")
    end

    if defined?(OpenSSL::PKey::EC)
      def ecdsa_sha2_nistp256_key
        @ecdsa_sha2_nistp256_key ||= OpenSSL::PKey::EC.new("-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEINv6pPVLlkqvT1v5MJlWgaSWGwqupISG4U79bUXQDNCaoAoGCCqGSM49\nAwEHoUQDQgAElqubvi/GkSme+bwtncU1NiE0dWQ0EO07VufUQg8lUJ5+Fi6f96qa\n95T1zwOMQhY1h8PP9rQIZr4S48vN/ZnQLw==\n-----END EC PRIVATE KEY-----\n")
      end

      def ecdsa_sha2_nistp384_key
        @ecdsa_sha2_nistp384_key ||= OpenSSL::PKey::EC.new("-----BEGIN EC PRIVATE KEY-----\nMIGkAgEBBDBxwkmydCn4mP4KMhlMpeBvIroQolWKVNoRPXpG7brFgK+Yiikqw8wd\nIZW5OlL4y3mgBwYFK4EEACKhZANiAARkoIR1oABi+aQJbKcmvzeYSKURQOyXM0HU\nR4T68v4hd/lJE4fFQRczj3wAaECe9u3CWI/oDlow4Vr0vab82ZGjIoblxblKQWYl\nyzENgzl226waGg1bLBo8Auilyf1B5yI=\n-----END EC PRIVATE KEY-----\n")
      end

      def ecdsa_sha2_nistp521_key
        @ecdsa_sha2_nistp521_key ||= OpenSSL::PKey::EC.new("-----BEGIN EC PRIVATE KEY-----\nMIHbAgEBBEHQ2i7kjEGQHQB4pUQW9a2eCLWR2S5Go8U3CDyfbRCrYEp/pTSgI8uu\nMXyR3bf3SjqFQgZ6MZk5lkyrissJuwmvZKAHBgUrgQQAI6GBiQOBhgAEAN14FACK\nbs/KTqw4rxijeozGTVJTh1hNzBl2XaIhM4Fv8o3fE/pvogymyFu53GCng6gC4dmx\n/hycF41iIM29xVKPAeBnRNl6MdFBjuthOmE8eCRezgk1Bak8aBDUrzNT8OQssscw\npvQK4nc6ga/wTDaQGy5kV8tCOHNs2wKH+p2LpWTJ\n-----END EC PRIVATE KEY-----\n")
      end
    end

    def anonymous_private_key
      @anonymous_key = <<-EOF
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC3id5gZ6bglJth
yli8JNaRxhsqKwwPlReEI/mplzz5IP6gWQ92LogXbdBXtHf9ZpA53BeLmtcNBEY0
Ygd7sPBhlHABS5D5///zltSSX2+L5GCEiC6dpfGsySjqymWF+SZ2PaqfZbkWLmCD
9u4ysueaHf7xbF6txGprNp69efttWxdy+vU5tno7HVxemMZQUalpShFrdAYKKXEo
cV7MtbkQjzubS14gaWGpWCXIl9uNKQeHpLKtre1Qn5Ft/zVpCHmhLQcYDuB1LAj9
7eoev4rIiOE2sfdkvKDlmFxvzq3myYH4o27WwAg9OZ5SBusn2zesKkRCBBEZ55rl
uVknOGHXAgMBAAECggEAZE0U2OxsNxkfXS6+lXswQ5PW7pF90towcsdSPgrniGIu
pKRnHbfKKbuaewOl+zZcpTIRL/rbgUKPtzrHSiJlC36aQyrvvJ/ZWV5ZJvC+vd19
nY/qob65NyrrkHwxRSjmiwGiR9/IaUXI+vUsMUqx5Ph1hawqhZ3sZlEAKR4LeDO8
M+OguG77jLaqj5/SNfi+GwyUDe85de4VfEG4S9HrMQk2Cp66rx0BqDnCLacyFQaI
R0VczMXTU52q0uETmgUr8G9A1SaRc5ZWKAfZwxJTvqdIImWC9E+CY7wm+mZD4FE6
iVzVC0ngcdEd596kTDdU2BPVMluWzLkfqIrTt/5CeQKBgQDzgRzCPNxFtai6RAIi
ekBSHqrDnrbeTaw32GVq5ACk1Zfk2I0svctz1iQ9qJ2SRINpygQhcyJKQ4r/LXi1
7Av9H/d6QV4T2AZzS4WcqBkxxRXFUfARtnKChzuCzNt9tNz4EZiv75RyQmztGZjV
i94+ZvCyqup5be4Svf4MBxin9QKBgQDA9P4nHzFWZakTMei78LGb/4Auc+r0rZp7
8xg8Z92tvrDeJjMdesdhiFrPP1qiSYHnQ81MSWpn6BycBsHZqitejQmYnYput/s4
qG+m7SrkN8WL6rijYsbB+U14VDjMlBlOgcEgjlSNU2oeS+68u+uVI/fgyXcXn4Jq
33TSWSgfGwKBgA2tRdE/G9wqfOShZ0FKfoxePpcoNfs8f5zPYbrkPYkEmjh3VU6b
Bm9mKrjv3JHXmU3608qRLe7f5lG42xvUu0OnZP4P59nTe2FEb6fB5VBfUn63wHUu
OzZLpDMPkJB59SNV0a6oFT1pr7aNhoEQDxaQL5rJcMwLOaEB3OAOEft1AoGASz7+
4Zi7b7rDPVYIMUpCqNfxT6wqovIUPWPmPqAuhXPIm0kAQ+2+VN2MtCc7m+/Ydawu
IiK7GPweNAY6kDxZH00WweolstmSYVzl9Y2lXUwWgGKvUB/T7I7g1Bzb7YOPftsA
ykZW2Kn/xwLLfdQ2oXleT82g4Jh2jmDHuMPF7qMCgYEA6QF45PvOgnrJessgmwO/
dEmkLl07PQYJPGZLaZteuWrvfMrn+AiW5aAdHzhzNaOtNy5B3T7zGUHtgxXegqgd
/QdCVCJgnZUO/zdAxkr22dDn+WEXkL4wgBVStQvvnQp9C2NJcoOExvex5PLzKWQg
WEKt5v3QsUEgVrzkM4K9UbI=
-----END PRIVATE KEY-----
      EOF
    end

    def encrypted(key, password)
      key.export(OpenSSL::Cipher::Cipher.new("des-ede3-cbc"), password)
    end

    def public(key)
      result = "#{key.ssh_type} "
      result << [Net::SSH::Buffer.from(:key, key).to_s].pack("m*").strip.tr("\n\r\t ", "")
      result << " joe@host.test"
    end
end
