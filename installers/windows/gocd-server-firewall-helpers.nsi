!define PROTOCOL_TCP 6
!define SCOPE_ALL_NETWORKS 0
!define IP_VERSION_IPV4_AND_IPV6 2
; the single quote around double quote is intentional. Some compiler weirdless!
!define REMOTE_ADDRESS_ALL '""'
!define FIREWALL_ENABLED_STATUS 1

Function "OpenFirewallPorts"
  ${LogText} "Opening firewall port 8153"
  ; SimpleFC::AddPort [port] [name] [protocol] [scope] [ip_version] [remote_addresses] [status]
  SimpleFC::AddPort 8153 "GoCD Server HTTP" ${PROTOCOL_TCP} ${SCOPE_ALL_NETWORKS} ${IP_VERSION_IPV4_AND_IPV6} ${REMOTE_ADDRESS_ALL} ${FIREWALL_ENABLED_STATUS}
  ${LogText} "Opening firewall port 8154"
  SimpleFC::AddPort 8154 "GoCD Server HTTPS" ${PROTOCOL_TCP} ${SCOPE_ALL_NETWORKS} ${IP_VERSION_IPV4_AND_IPV6} ${REMOTE_ADDRESS_ALL} ${FIREWALL_ENABLED_STATUS}
FunctionEnd

Function "un.DisableFirewallPorts"
  ; SimpleFC::RemovePort [port] [protocol]
  ${LogText} "Closing firewall port 8153"
  SimpleFC::RemovePort 8153 ${PROTOCOL_TCP}
  ${LogText} "Closing firewall port 8154"
  SimpleFC::RemovePort 8154 ${PROTOCOL_TCP}
FunctionEnd
