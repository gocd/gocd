##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

class ServerConfigurationForm
  include ApplicationHelper
  include JavaImports

  PURGING_ENABLED = "Size"

  attr_accessor :hostName, :port, :username, :password, :encrypted_password, :tls, :from, :adminMail, :password_changed,
                :allow_auto_login, :ldap_username, :ldap_uri, :ldap_password, :ldap_encrypted_password, :ldap_password_changed, :ldap_search_base, :ldap_search_filter, :password_file_path,
                :artifactsDir, :purgeArtifacts, :jobTimeout, :timeoutType, :siteUrl, :secureSiteUrl, :commandRepositoryLocation
  
  def initialize(attributes)
    @hostName, @port, @username, @password, @encrypted_password, @password_changed, @tls, @from, @adminMail =
            attributes[:hostName], attributes[:port], attributes[:username], attributes[:password], attributes[:encrypted_password], attributes[:password_changed], attributes[:tls], attributes[:from], attributes[:adminMail]

    @allow_auto_login, @ldap_username, @ldap_uri, @ldap_password, @ldap_encrypted_password, @ldap_password_changed, @ldap_search_base, @ldap_search_filter, @password_file_path =
            attributes[:allow_auto_login], attributes[:ldap_username], attributes[:ldap_uri], attributes[:ldap_password], attributes[:ldap_encrypted_password], attributes[:ldap_password_changed],
                    attributes[:ldap_search_base], attributes[:ldap_search_filter], attributes[:password_file_path]
    @artifactsDir, @purgeArtifacts, @purgeStart, @purgeUpto = attributes[:artifactsDir], attributes[:purgeArtifacts], attributes[:purgeStart], attributes[:purgeUpto]
    @jobTimeout = attributes[:timeoutType] == com.thoughtworks.go.config.ServerConfig::NEVER_TIMEOUT ? "0" : attributes[:jobTimeout]
    @timeoutType = attributes[:timeoutType]
    @siteUrl, @secureSiteUrl =  attributes[:siteUrl], attributes[:secureSiteUrl]
    @commandRepositoryLocation = attributes[:commandRepositoryLocation]
  end

  def self.artifactPurgingAllowed?(server_config)
    server_config.isArtifactPurgingAllowed() ? PURGING_ENABLED : "Never"
  end

  def self.from_server_config(server_config)
    security_config, mail_host = server_config.security(),server_config.mailHost()
    ldap_config = security_config.ldapConfig()
    password_file_path = security_config.passwordFileConfig().path()
    auto_login = security_config.isAllowOnlyKnownUsersToLogin() ? "false" : "true"

    allow_auto_login = {:allow_auto_login => auto_login}
    mail_host_params= {:hostName => mail_host.getHostName(), :port => mail_host.getPort().to_s, :username => mail_host.getUserName(),
                       :password => mail_host.getPassword(), :encrypted_password => mail_host.getEncryptedPassword(), :password_changed => mail_host.isPasswordChanged().to_s , :tls => mail_host.getTls().to_s, :from => mail_host.getFrom(), :adminMail => mail_host.getAdminMail()}
    ldap_config_params = {:ldap_uri => ldap_config.uri(), :ldap_username => ldap_config.managerDn(), :ldap_password => ldap_config.managerPassword(), :ldap_encrypted_password => ldap_config.getEncryptedManagerPassword(), :ldap_password_changed => ldap_config.isPasswordChanged().to_s,
                          :ldap_search_base => from_bases_collection(ldap_config.searchBases()), :ldap_search_filter => ldap_config.searchFilter(), :password_file_path => password_file_path}
    artifacts_params = {:artifactsDir => server_config.artifactsDir(), :purgeArtifacts => artifactPurgingAllowed?(server_config), :purgeStart => server_config.getPurgeStart(), :purgeUpto => server_config.getPurgeUpto()}
    job_timeout_params = {:timeoutType => server_config.getTimeoutType(), :jobTimeout => server_config.getJobTimeout()}
    site_url_params = {:siteUrl => server_config.getSiteUrl().getUrl(), :secureSiteUrl => server_config.getSecureSiteUrl().getUrl()}
    command_repo_location = {:commandRepositoryLocation => server_config.getCommandRepositoryLocation()}
    ServerConfigurationForm.new(mail_host_params.merge(ldap_config_params).merge(allow_auto_login).merge(artifacts_params).merge(job_timeout_params).merge(site_url_params).merge(command_repo_location))
  end

  def to_mail_host
    if is_empty_mailhost
      return MailHost.new(com.thoughtworks.go.security.GoCipher.new)
    end
    MailHost.new(hostName, Integer(port), username, password, encrypted_password, password_changed == "true", tls == "true", from, adminMail)
  end

  def to_ldap_config
    LdapConfig.new(ldap_uri, ldap_username, ldap_password, ldap_encrypted_password, ldap_password_changed == "true", to_bases_collection(ldap_search_base), ldap_search_filter)
  end

  def self.from_bases_collection bases_collection
    bases_for_display = Array.new
    bases_collection.each do |base|
      bases_for_display << base.getValue()
    end
    bases_for_display.join("\r\n")
  end

  def to_bases_collection new_line_separated_bases
    bases = BasesConfig.new
    unless new_line_separated_bases.blank?
      new_line_separated_bases.split(/\r?\n/).each do |value|
        bases.add(BaseConfig.new(value)) unless value.blank?
      end
    end
    bases
  end

  def to_password_file_config
    PasswordFileConfig.new(password_file_path)
  end

  def should_allow_auto_login
    allow_auto_login != "false"
  end

  def to_security
    SecurityConfig.new(to_ldap_config, to_password_file_config, !should_allow_auto_login)
  end

  def purging_allowed?
    @purgeArtifacts == PURGING_ENABLED
  end

  def purgeStart
    purging_allowed? ? @purgeStart.to_f : nil
  end

  def siteUrl
    @siteUrl
  end

  def secureSiteUrl
    @secureSiteUrl
  end

  def purgeUpto
    purging_allowed? ? @purgeUpto.to_f : nil
  end

  def is_empty_mailhost
    [hostName, from, adminMail].all? { |param| param.blank? } and (port.blank? || port == "0")
  end

  def validate(result)
    if (is_empty_mailhost())
      return true
    end
    if !hostName
      result.badRequest(LocalizedMessage.string("HOSTNAME_REQUIRED"))
      return false
    end
    if !port
      result.badRequest(LocalizedMessage.string("PORT_REQUIRED"))
      return false
    end
    if !from
      result.badRequest(LocalizedMessage.string("FROM_ADDRESS_REQUIRED"))
      return false
    end
    if !adminMail
      result.badRequest(LocalizedMessage.string("ADMIN_ADDRESS_REQUIRED"))
      return false
    end
    if !number?(port)
      result.badRequest(LocalizedMessage.string("INVALID_PORT"))
      return false
    end
    if tls !~ /true|false/
      result.badRequest(LocalizedMessage.string("INVALID_TLS"))
      return false
    end
    true
  end

  def ==(other)
    hostName == other.hostName &&
    port == other.port &&
    username == other.username &&
    password == other.password &&
    tls == other.tls &&
    from == other.from &&
    adminMail == other.adminMail &&
    allow_auto_login == other.allow_auto_login &&
    ldap_username == other.ldap_username &&
    ldap_uri == other.ldap_uri &&
    ldap_password == other.ldap_password &&
    ldap_search_base == other.ldap_search_base &&
    ldap_search_filter == other.ldap_search_filter &&
    password_file_path == other.password_file_path
  end

  def hash
    hostName ^ port ^ username ^ password ^ tls ^ from ^ adminMail ^ allow_auto_login ^ ldap_username ^ ldap_uri ^ ldap_password ^ ldap_search_base ^ ldap_search_filter ^ password_file_path
  end
end

