require 'common'
require 'net/ssh/authentication/key_manager'

module Authentication

  class TestKeyManager < Test::Unit::TestCase
    def test_key_files_and_known_identities_are_empty_by_default
      assert manager.key_files.empty?
      assert manager.known_identities.empty?
    end

    def test_assume_agent_is_available_by_default
      assert manager.use_agent?
    end

    def test_add_ensures_list_is_unique
      manager.add "/first"
      manager.add "/second"
      manager.add "/third"
      manager.add "/second"
      assert_true manager.key_files.length == 3
      final_files = manager.key_files.map {|item| item.split('/').last}
      assert_equal %w(first second third), final_files
    end

    def test_use_agent_should_be_set_to_false_if_agent_could_not_be_found
      Net::SSH::Authentication::Agent.expects(:connect).raises(Net::SSH::Authentication::AgentNotAvailable)
      assert manager.use_agent?
      assert_nil manager.agent
      assert !manager.use_agent?
    end

    def test_each_identity_should_load_from_key_files
      manager.stubs(:agent).returns(nil)
      first = File.expand_path("/first")
      second = File.expand_path("/second")
      stub_file_private_key first, rsa
      stub_file_private_key second, dsa      

      identities = []
      manager.each_identity { |identity| identities << identity }

      assert_equal 2, identities.length
      assert_equal rsa.to_blob, identities.first.to_blob
      assert_equal dsa.to_blob, identities.last.to_blob
      
      assert_equal({:from => :file, :file => first, :key => rsa}, manager.known_identities[rsa])
      assert_equal({:from => :file, :file => second, :key => dsa}, manager.known_identities[dsa])
    end

    def test_identities_should_load_from_agent
      manager.stubs(:agent).returns(agent)

      identities = []
      manager.each_identity { |identity| identities << identity }

      assert_equal 2, identities.length
      assert_equal rsa.to_blob, identities.first.to_blob
      assert_equal dsa.to_blob, identities.last.to_blob

      assert_equal({:from => :agent}, manager.known_identities[rsa])
      assert_equal({:from => :agent}, manager.known_identities[dsa])
    end

    if defined?(OpenSSL::PKey::EC)
      def test_identities_with_ecdsa_should_load_from_agent
        manager.stubs(:agent).returns(agent_with_ecdsa_keys)

        identities = []
        manager.each_identity { |identity| identities << identity }
        assert_equal 5, identities.length

        assert_equal rsa.to_blob, identities[0].to_blob
        assert_equal dsa.to_blob, identities[1].to_blob
        assert_equal ecdsa_sha2_nistp256.to_blob, identities[2].to_blob
        assert_equal ecdsa_sha2_nistp384.to_blob, identities[3].to_blob
        assert_equal ecdsa_sha2_nistp521.to_blob, identities[4].to_blob

        assert_equal({:from => :agent}, manager.known_identities[rsa])
        assert_equal({:from => :agent}, manager.known_identities[dsa])
        assert_equal({:from => :agent}, manager.known_identities[ecdsa_sha2_nistp256])
        assert_equal({:from => :agent}, manager.known_identities[ecdsa_sha2_nistp384])
        assert_equal({:from => :agent}, manager.known_identities[ecdsa_sha2_nistp521])
      end
    end

    def test_only_identities_with_key_files_should_load_from_agent_of_keys_only_set
      manager(:keys_only => true).stubs(:agent).returns(agent)

      first = File.expand_path("/first")
      stub_file_private_key first, rsa

      identities = []
      manager.each_identity { |identity| identities << identity }

      assert_equal 1, identities.length
      assert_equal rsa.to_blob, identities.first.to_blob

      assert_equal({:from => :agent}, manager.known_identities[rsa])
    end

    def test_identities_without_public_key_files_should_not_be_touched_if_identity_loaded_from_agent
      manager.stubs(:agent).returns(agent)

      first = File.expand_path("/first")
      stub_file_public_key  first, rsa
      second = File.expand_path("/second")
      stub_file_private_key second, dsa, :passphrase => :should_not_be_asked

      identities = []
      manager.each_identity do |identity|
        identities << identity
        break if manager.known_identities[identity][:from] == :agent
      end

      assert_equal 1, identities.length
      assert_equal rsa.to_blob, identities.first.to_blob
    end

    def test_sign_with_agent_originated_key_should_request_signature_from_agent
      manager.stubs(:agent).returns(agent)
      manager.each_identity { |identity| } # preload the known_identities
      agent.expects(:sign).with(rsa, "hello, world").returns("abcxyz123")
      assert_equal "abcxyz123", manager.sign(rsa, "hello, world")
    end

    def test_sign_with_file_originated_key_should_load_private_key_and_sign_with_it
      manager.stubs(:agent).returns(nil)
      first = File.expand_path("/first")
      stub_file_private_key first, rsa(512)
      rsa.expects(:ssh_do_sign).with("hello, world").returns("abcxyz123")
      manager.each_identity { |identity| } # preload the known_identities
      assert_equal "\0\0\0\assh-rsa\0\0\0\011abcxyz123", manager.sign(rsa, "hello, world")
    end

    def test_sign_with_file_originated_key_should_raise_key_manager_error_if_unloadable
      manager.known_identities[rsa] = { :from => :file, :file => "/first" }

      Net::SSH::KeyFactory.expects(:load_private_key).raises(OpenSSL::PKey::RSAError)

      assert_raises Net::SSH::Authentication::KeyManagerError do
        manager.sign(rsa, "hello, world")
      end
    end

    private

      def stub_file_private_key(name, key, options = {})
        manager.add(name)
        File.stubs(:readable?).with(name).returns(true)
        File.stubs(:readable?).with(name + ".pub").returns(false)

        case options.fetch(:passphrase, :indifferently)
        when :should_be_asked
          Net::SSH::KeyFactory.expects(:load_private_key).with(name, nil, false).raises(OpenSSL::PKey::RSAError).at_least_once
          Net::SSH::KeyFactory.expects(:load_private_key).with(name, nil, true).returns(key).at_least_once
        when :should_not_be_asked
          Net::SSH::KeyFactory.expects(:load_private_key).with(name, nil, false).raises(OpenSSL::PKey::RSAError).at_least_once
          Net::SSH::KeyFactory.expects(:load_private_key).with(name, nil, true).never
        else # :indifferently
          Net::SSH::KeyFactory.expects(:load_private_key).with(name, nil, any_of(true, false)).returns(key).at_least_once
        end

        # do not override OpenSSL::PKey::EC#public_key
        # (it will be called in transport/openssl.rb.)
        unless defined?(OpenSSL::PKey::EC) && key.public_key.kind_of?(OpenSSL::PKey::EC::Point)
          key.stubs(:public_key).returns(key)
        end
      end

      def stub_file_public_key(name, key)
        manager.add(name)
        File.stubs(:readable?).with(name).returns(false)
        File.stubs(:readable?).with(name + ".pub").returns(true)

        Net::SSH::KeyFactory.expects(:load_public_key).with(name + ".pub").returns(key).at_least_once
      end

      def rsa(size=512)
        @rsa ||= OpenSSL::PKey::RSA.new(size)
      end

      def dsa
        @dsa ||= OpenSSL::PKey::DSA.new(512)
      end

      if defined?(OpenSSL::PKey::EC)
        def ecdsa_sha2_nistp256
          @ecdsa_sha2_nistp256 ||= OpenSSL::PKey::EC.new("prime256v1").generate_key
        end

        def ecdsa_sha2_nistp384
          @ecdsa_sha2_nistp384 ||= OpenSSL::PKey::EC.new("secp384r1").generate_key
        end

        def ecdsa_sha2_nistp521
          @ecdsa_sha2_nistp521 ||= OpenSSL::PKey::EC.new("secp521r1").generate_key
        end
      end

      def agent
        @agent ||= stub("agent", :identities => [rsa, dsa])
      end

      def agent_with_ecdsa_keys
        @agent ||= stub("agent", :identities => [rsa, dsa,
                                                 ecdsa_sha2_nistp256,
                                                 ecdsa_sha2_nistp384,
                                                 ecdsa_sha2_nistp521])
      end

      def manager(options = {})
        @manager ||= Net::SSH::Authentication::KeyManager.new(nil, options)
      end

  end

end
