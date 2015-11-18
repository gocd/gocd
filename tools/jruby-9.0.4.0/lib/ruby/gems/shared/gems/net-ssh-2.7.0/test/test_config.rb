require 'common'
require 'net/ssh/config'
require 'pathname'

class TestConfig < Test::Unit::TestCase
  def test_home_should_be_absolute_path
    assert Pathname.new(ENV['HOME']).absolute?
  end

  def test_load_for_non_existant_file_should_return_empty_hash
    bogus_file = File.expand_path("/bogus/file")
    File.expects(:readable?).with(bogus_file).returns(false)
    assert_equal({}, Net::SSH::Config.load(bogus_file, "host.name"))
  end

  def test_load_should_expand_path
    expected = File.expand_path("~/.ssh/config")
    File.expects(:readable?).with(expected).returns(false)
    Net::SSH::Config.load("~/.ssh/config", "host.name")
  end

  def test_load_with_exact_host_match_should_load_that_section
    config = Net::SSH::Config.load(config(:exact_match), "test.host")
    assert config['compression']
    assert config['forwardagent']
    assert_equal 1234, config['port']
  end

  def test_load_with_wild_card_matches_should_load_all_matches_with_first_match_taking_precedence
    config = Net::SSH::Config.load(config(:wild_cards), "test.host")
    assert_equal 1234, config['port']
    assert !config['compression']
    assert config['forwardagent']
    assert_equal %w(~/.ssh/id_dsa), config['identityfile']
    assert !config.key?('rekeylimit')
  end

  def test_for_should_load_all_files_and_translate_to_net_ssh_options
    config = Net::SSH::Config.for("test.host", [config(:exact_match), config(:wild_cards)])
    assert_equal 1234, config[:port]
    assert config[:compression]
    assert config[:forward_agent]
    assert_equal %w(~/.ssh/id_dsa), config[:keys]
    assert !config.key?(:rekey_limit)
  end
  
  def test_load_with_no_host
    config = Net::SSH::Config.load(config(:nohost), "test.host")
    assert_equal %w(~/.ssh/id_dsa ~/.ssh/id_rsa), config['identityfile']
    assert_equal 1985, config['port']
  end
  
  def test_load_with_multiple_hosts
    config = Net::SSH::Config.load(config(:multihost), "test.host")
    assert config['compression']
    assert_equal '2G', config['rekeylimit']
    assert_equal 1980, config['port']
  end
  
  def test_load_with_multiple_hosts_and_config_should_match_for_both
    aconfig = Net::SSH::Config.load(config(:multihost), "test.host")
    bconfig = Net::SSH::Config.load(config(:multihost), "other.host")
    assert_equal aconfig['port'], bconfig['port']
    assert_equal aconfig['compression'], bconfig['compression']
    assert_equal aconfig['rekeylimit'], bconfig['rekeylimit']
  end
  
  def test_load_should_parse_equal_sign_delimiters
    config = Net::SSH::Config.load(config(:eqsign), "test.test")
    assert config['compression']
    assert_equal 1234, config['port']
  end

  def test_translate_should_correctly_translate_from_openssh_to_net_ssh_names
    open_ssh = {
      'bindaddress'             => "127.0.0.1",
      'ciphers'                 => "a,b,c",
      'compression'             => true,
      'compressionlevel'        => 6,
      'connecttimeout'          => 100,
      'forwardagent'            => true,
      'hostbasedauthentication' => true,
      'hostkeyalgorithms'       => "d,e,f",
      'identityfile'            => %w(g h i),
      'macs'                    => "j,k,l",
      'passwordauthentication'  => true,
      'port'                    => 1234,
      'pubkeyauthentication'    => true,
      'rekeylimit'              => 1024,
      'sendenv'                 => "LC_*"
    }

    net_ssh = Net::SSH::Config.translate(open_ssh)

    assert_equal %w(a b c), net_ssh[:encryption]
    assert_equal true,      net_ssh[:compression]
    assert_equal 6,         net_ssh[:compression_level]
    assert_equal 100,       net_ssh[:timeout]
    assert_equal true,      net_ssh[:forward_agent]
    assert_equal %w(hostbased password publickey), net_ssh[:auth_methods].sort
    assert_equal %w(d e f), net_ssh[:host_key]
    assert_equal %w(g h i), net_ssh[:keys]
    assert_equal %w(j k l), net_ssh[:hmac]
    assert_equal 1234,      net_ssh[:port]
    assert_equal 1024,      net_ssh[:rekey_limit]
    assert_equal "127.0.0.1", net_ssh[:bind_address]
    assert_equal [/^LC_.*$/], net_ssh[:send_env]
  end
  
  def test_load_with_plus_sign_hosts
    config = Net::SSH::Config.load(config(:host_plus), "test.host")
    assert config['compression']
  end
  
  def test_load_with_numeric_host
    config = Net::SSH::Config.load(config(:numeric_host), "1234")
    assert config['compression']
    assert_equal '2G', config['rekeylimit']
    assert_equal 1980, config['port']
  end

  def test_load_wildcar_with_substitutes
    config = Net::SSH::Config.load(config(:substitutes), "toto")
    net_ssh = Net::SSH::Config.translate(config)
    assert_equal 'toto', net_ssh[:host_name]
  end

  def test_load_sufix_with_substitutes
    config = Net::SSH::Config.load(config(:substitutes), "test")
    net_ssh = Net::SSH::Config.translate(config)
    assert_equal 'test.sufix', net_ssh[:host_name]
  end

  def test_load_prefix_and_sufix_with_substitutes
    config = Net::SSH::Config.load(config(:substitutes), "1234")
    net_ssh = Net::SSH::Config.translate(config)
    assert_equal 'prefix.1234.sufix', net_ssh[:host_name]
  end

  def test_load_with_send_env
    config = Net::SSH::Config.load(config(:send_env), "1234")
    net_ssh = Net::SSH::Config.translate(config)
    assert_equal [/^GIT_.*$/, /^LANG$/, /^LC_.*$/], net_ssh[:send_env]
  end

  private

    def config(name)
      "test/configs/#{name}"
    end
end
