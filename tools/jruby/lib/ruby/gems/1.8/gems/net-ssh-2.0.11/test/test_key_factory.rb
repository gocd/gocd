require 'common'
require 'net/ssh/key_factory'

class TestKeyFactory < Test::Unit::TestCase
  def test_load_unencrypted_private_RSA_key_should_return_key
    File.expects(:read).with("/key-file").returns(rsa_key.export)
    assert_equal rsa_key.to_der, Net::SSH::KeyFactory.load_private_key("/key-file").to_der
  end

  def test_load_unencrypted_private_DSA_key_should_return_key
    File.expects(:read).with("/key-file").returns(dsa_key.export)
    assert_equal dsa_key.to_der, Net::SSH::KeyFactory.load_private_key("/key-file").to_der
  end

  def test_load_encrypted_private_RSA_key_should_prompt_for_password_and_return_key
    File.expects(:read).with("/key-file").returns(encrypted(rsa_key, "password"))
    Net::SSH::KeyFactory.expects(:prompt).with("Enter passphrase for /key-file:", false).returns("password")
    assert_equal rsa_key.to_der, Net::SSH::KeyFactory.load_private_key("/key-file").to_der
  end

  def test_load_encrypted_private_RSA_key_with_password_should_not_prompt_and_return_key
    File.expects(:read).with("/key-file").returns(encrypted(rsa_key, "password"))
    assert_equal rsa_key.to_der, Net::SSH::KeyFactory.load_private_key("/key-file", "password").to_der
  end

  def test_load_encrypted_private_DSA_key_should_prompt_for_password_and_return_key
    File.expects(:read).with("/key-file").returns(encrypted(dsa_key, "password"))
    Net::SSH::KeyFactory.expects(:prompt).with("Enter passphrase for /key-file:", false).returns("password")
    assert_equal dsa_key.to_der, Net::SSH::KeyFactory.load_private_key("/key-file").to_der
  end

  def test_load_encrypted_private_DSA_key_with_password_should_not_prompt_and_return_key
    File.expects(:read).with("/key-file").returns(encrypted(dsa_key, "password"))
    assert_equal dsa_key.to_der, Net::SSH::KeyFactory.load_private_key("/key-file", "password").to_der
  end

  def test_load_encrypted_private_key_should_give_three_tries_for_the_password_and_then_raise_exception
    File.expects(:read).with("/key-file").returns(encrypted(rsa_key, "password"))
    Net::SSH::KeyFactory.expects(:prompt).times(3).with("Enter passphrase for /key-file:", false).returns("passwod","passphrase","passwd")
    assert_raises(OpenSSL::PKey::RSAError) { Net::SSH::KeyFactory.load_private_key("/key-file") }
  end

  def test_load_public_rsa_key_should_return_key
    File.expects(:read).with("/key-file").returns(public(rsa_key))
    assert_equal rsa_key.to_blob, Net::SSH::KeyFactory.load_public_key("/key-file").to_blob
  end

  private

    def rsa_key
      @rsa_key ||= OpenSSL::PKey::RSA.new("0@\002\001\000\002\t\000\300\030\317\2132\340 \267\002\003\001\000\001\002\t\000\236~\232\025\350Y=\341\002\005\000\352D\217\a\002\005\000\321\352\304\321\002\005\000\242\350\206%\002\005\000\270\021\217\361\002\004~\253\214j")
    end

    def dsa_key
      @dsa_key ||= OpenSSL::PKey::DSA.new("0\201\367\002\001\000\002A\000\203\316/\037u\272&J\265\003l3\315d\324h\372{\t8\252#\331_\026\006\035\270\266\255\343\353Z\302\276\335\336\306\220\375\202L\244\244J\206>\346\b\315\211\302L\246x\247u\a\376\366\345\302\016#\002\025\000\244\274\302\221Og\275/\302+\356\346\360\024\373wI\2573\361\002@\027\215\270r*\f\213\350C\245\021:\350 \006\\\376\345\022`\210b\262\3643\023XLKS\320\370\002\276\347A\nU\204\276\324\256`=\026\240\330\306J\316V\213\024\e\030\215\355\006\037q\337\356ln\002@\017\257\034\f\260\333'S\271#\237\230E\321\312\027\021\226\331\251Vj\220\305\316\036\v\266+\000\230\270\177B\003?t\a\305]e\344\261\334\023\253\323\251\223M\2175)a(\004\"lI8\312\303\307\a\002\024_\aznW\345\343\203V\326\246ua\203\376\201o\350\302\002")
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