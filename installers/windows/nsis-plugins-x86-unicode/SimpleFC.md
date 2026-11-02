# NSIS Simple Firewall Plugin (SimpleFC)

This plugin can be used to configure the Windows Firewall from an NSIS installer.
It contains functions to enable, check, add or remove programs or ports to the
firewall exception list. It also contains functions for checking the firewall
status, enabling or disabling the firewall and so on.

## Download

Download the latest version from the [Releases](../../releases/latest) page.
Each release contains two packages:

| Package | NSIS | Copy `SimpleFC.dll` to |
| --- | --- | --- |
| `NSIS_Simple_Firewall_Plugin_<version>_ANSI.zip` | NSIS 2, NSIS 3 (ANSI installer) | `Plugins\x86-ansi` (NSIS 3) or `Plugins` (NSIS 2) |
| `NSIS_Simple_Firewall_Plugin_<version>_Unicode.zip` | NSIS 3 with `Unicode true` | `Plugins\x86-unicode` |

Both packages provide the same functions. Earlier versions than 1.21 are not available.

## Reference

```nsis
SimpleFC::EnableDisableFirewall [status]
SimpleFC::IsFirewallEnabled

SimpleFC::AllowDisallowExceptionsNotAllowed [status]
SimpleFC::AreExceptionsNotAllowed

SimpleFC::EnableDisableNotifications [status]
SimpleFC::AreNotificationsEnabled

SimpleFC::StartStopFirewallService [status]
SimpleFC::IsFirewallServiceRunning

SimpleFC::AddPort [port] [name] [protocol] [scope] [ip_version] [remote_addresses] [status]
SimpleFC::IsPortAdded [port] [protocol]
SimpleFC::RemovePort [port] [protocol]

SimpleFC::IsPortEnabled [port] [protocol]
SimpleFC::EnableDisablePort [port] [protocol]

SimpleFC::AddApplication [name] [path] [scope] [ip_version] [remote_addresses] [status]
SimpleFC::IsApplicationAdded [path]
SimpleFC::RemoveApplication [path]

SimpleFC::IsApplicationEnabled [path]
SimpleFC::EnableDisableApplication [path]

SimpleFC::RestoreDefaults

SimpleFC::AllowDisallowIcmpOutboundDestinationUnreachable [status]
SimpleFC::AllowDisallowIcmpRedirect [status]
SimpleFC::AllowDisallowIcmpInboundEchoRequest [status]
SimpleFC::AllowDisallowIcmpOutboundTimeExceeded [status]
SimpleFC::AllowDisallowIcmpOutboundParameterProblem [status]
SimpleFC::AllowDisallowIcmpOutboundSourceQuench [status]
SimpleFC::AllowDisallowIcmpInboundRouterRequest [status]
SimpleFC::AllowDisallowIcmpInboundTimestampRequest [status]
SimpleFC::AllowDisallowIcmpInboundMaskRequest [status]
SimpleFC::AllowDisallowIcmpOutboundPacketTooBig [status]
SimpleFC::IsIcmpTypeAllowed [ip_version] [local_address] [icmp_type]

SimpleFC::AdvAddRule [name] [description] [protocol] [direction]
  [status] [profile] [action] [application] [service_name] [icmp_types_and_codes]
  [group] [local_ports] [remote_ports] [local_address] [remote_address]
SimpleFC::AdvRemoveRule [name]
SimpleFC::AdvExistsRule [name]
```

## Parameters

**`port`** – TCP/UDP port which should be opened/closed

**`name`** – The name of the application/port/rule

**`description`** – Description of the rule

**`protocol`** – One of the following protocols

| Value | Protocol |
| --- | --- |
| 1 | ICMPv4 |
| 6 | TCP |
| 17 | UDP |
| 58 | ICMPv6 |
| 256 | ANY |

**`scope`** – One of the following scopes

| Value | Scope |
| --- | --- |
| 0 | All networks |
| 1 | Only local subnets |
| 2 | Custom scope |
| 3 | Max |

Note: If you use custom scope you must define `remote_addresses`.

**`ip_version`**

| Value | IP version |
| --- | --- |
| 0 | IPv4 |
| 1 | IPv6 |
| 2 | Any version |

**`icmp_type`**

| Value | ICMP type |
| --- | --- |
| 3 | Outbound Destination Unreachable (ICMPv4) |
| 4 | Outbound Source Quench (ICMPv4) |
| 5 | Redirect (ICMPv4) |
| 8 | Inbound Echo Request (ICMPv4) |
| 9 | Inbound Router Request (ICMPv4) |
| 11 | Outbound Time Exceeded (ICMPv4) |
| 12 | Outbound Parameter Problem (ICMPv4) |
| 13 | Inbound Timestamp Request (ICMPv4) |
| 17 | Inbound Mask Request (ICMPv4) |
| 1 | Outbound Destination Unreachable (ICMPv6) |
| 2 | Outbound Packet Too Big (ICMPv6) |
| 3 | Outbound Time Exceeded (ICMPv6) |
| 4 | Outbound Parameter Problem (ICMPv6) |
| 128 | Inbound Echo Request (ICMPv6) |
| 137 | Redirect (ICMPv6) |

**`direction`**

| Value | Direction |
| --- | --- |
| 1 | In |
| 2 | Out |

**`profile`**

| Value | Profile |
| --- | --- |
| 1 | Domain |
| 2 | Private |
| 4 | Public |
| 2147483647 | All profiles |

**`action`**

| Value | Action |
| --- | --- |
| 0 | Block |
| 1 | Allow |

**`application`** – Path of the application (can be empty)

**`service_name`** – Specifies the service name property of the application (can be empty).
Note: A service name value of `*` indicates that a service, not an application, must be sending or receiving traffic.

**`icmp_types_and_codes`** – Specified ICMP types and codes (can be empty)

**`group`** – Put the rule in this specified group (can be empty).
Note: On Vista the group must be a resource string in an exe/dll, e.g. `@C:\Program Files\My Application\myapp.exe,-10000`.
On all other operating systems it can be a string value.

**`local_ports`** – Local ports (the protocol property must be set before, otherwise can be empty).
Note: The following port keywords are additionally valid: `RPC`, `RPC-EPMap`, `Teredo`, `IPTLSIn`, `IPHTTPSIn` and `Ply2Disc`

**`remote_ports`** – Remote ports (the protocol property must be set before, otherwise can be empty).
Note: The following port keywords are additionally valid: `IPTLSOut`, `IPHTTPSOUT`

**`local_address`** – Local addresses from which the application can listen for traffic (can be empty)

**`remote_addresses`** – Remote addresses from which the port can listen for traffic (can be empty)

**`status`** – Status of the port, application, rule, firewall or service, for example enabled/disabled, start/stop or allow/disallow

| Value | Status |
| --- | --- |
| 0 | Disabled, stop or disallow |
| 1 | Enabled, start or allow |

## Examples

### Most Used Functions

In this script you can find the two most used functions. If you are searching for
some special firewall exceptions please look at [All Functions](#all-functions).

```nsis
; Add an application to the firewall exception list - All Networks - All IP Version - Enabled
  SimpleFC::AddApplication "My Application" "PathToApplication" 0 2 "" 1
  Pop $0 ; return error(1)/success(0)

; Remove an application from the firewall exception list
  SimpleFC::RemoveApplication "PathToApplication"
  Pop $0 ; return error(1)/success(0)
```

### All Functions

In this script you can find the examples of all functions provided by this plugin.

```nsis
; Add the port 37/TCP to the firewall exception list - All Networks - All IP Version - Enabled
  SimpleFC::AddPort 37 "My Application" 6 0 2 "" 1
  Pop $0 ; return error(1)/success(0)

; Check if the port 37/TCP is added to the firewall exception list
  SimpleFC::IsPortAdded 37 6
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=Added/0=Not added

; Remove the port 37/TCP from the firewall exception list
  SimpleFC::RemovePort 37 6
  Pop $0 ; return error(1)/success(0)

; Check if the port 37/TCP is enabled/disabled
  SimpleFC::IsPortEnabled 37 6
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=Enabled/0=Not enabled

; Disable the port 37/TCP
  SimpleFC::EnableDisablePort 37 6 0
  Pop $0 ; return error(1)/success(0)

; Enable the port 37/TCP
  SimpleFC::EnableDisablePort 37 6 1
  Pop $0 ; return error(1)/success(0)

; Check if an application is enabled/disabled
  SimpleFC::IsApplicationEnabled "PathToApplication"
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=Enabled/0=Not enabled

; Disable the application
  SimpleFC::EnableDisableApplication "PathToApplication" 0
  Pop $0 ; return error(1)/success(0)

; Enable the application
  SimpleFC::EnableDisableApplication "PathToApplication" 1
  Pop $0 ; return error(1)/success(0)

; Add an application to the firewall exception list - All Networks - All IP Version - Enabled
  SimpleFC::AddApplication "My Application" "PathToApplication" 0 2 "" 1
  Pop $0 ; return error(1)/success(0)

; Check if the application is added to the firewall exception list
  SimpleFC::IsApplicationAdded "PathToApplication"
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=Added/0=Not added

; Remove an application from the firewall exception list
  SimpleFC::RemoveApplication "PathToApplication"
  Pop $0 ; return error(1)/success(0)

; Disable the windows firewall
  SimpleFC::EnableDisableFirewall 0
  Pop $0 ; return error(1)/success(0)

; Enable the windows firewall
  SimpleFC::EnableDisableFirewall 1
  Pop $0 ; return error(1)/success(0)

; Check if the firewall is enabled
  SimpleFC::IsFirewallEnabled
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=Enabled/0=Disabled

; Enable exceptions are not allowed on the windows firewall
  SimpleFC::AllowDisallowExceptionsNotAllowed 1
  Pop $0 ; return error(1)/success(0)

; Disable exceptions are not allowed on the windows firewall
  SimpleFC::AllowDisallowExceptionsNotAllowed 0
  Pop $0 ; return error(1)/success(0)

; Check if exceptions are not allowed
  SimpleFC::AreExceptionsNotAllowed
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=Exceptions are not allowed is activated/0=Exception are not allowed is deactivated

; Enable notifications on the windows firewall
  SimpleFC::EnableDisableNotifications 1

; Disable notifications on the windows firewall
  SimpleFC::EnableDisableNotifications 0
  Pop $0 ; return error(1)/success(0)

; Check if notifications are enabled/disabled
  SimpleFC::AreNotificationsEnabled
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=Enabled/0=Disabled

; Starts the windows firewall service
  SimpleFC::StartStopFirewallService 1
  Pop $0 ; return error(1)/success(0)

; Stops the windows firewall service
  SimpleFC::StartStopFirewallService 0
  Pop $0 ; return error(1)/success(0)

; Check if windows firewall service is running
  SimpleFC::IsFirewallServiceRunning
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=IsRunning/0=Not Running

; Sets the windows firewall to default settings
  SimpleFC::RestoreDefaults
  Pop $0 ; return error(1)/success(0)

; Enable ICMP outbound destination unreachable state
  SimpleFC::AllowDisallowIcmpOutboundDestinationUnreachable 1
  Pop $0 ; return error(1)/success(0)

; Enable ICMP redirect state
  SimpleFC::AllowDisallowIcmpRedirect 1
  Pop $0 ; return error(1)/success(0)

; Enable ICMP inbound echo request
  SimpleFC::AllowDisallowIcmpInboundEchoRequest 1
  Pop $0 ; return error(1)/success(0)

; Enable ICMP outbound time exceeded
  SimpleFC::AllowDisallowIcmpOutboundTimeExceeded 1
  Pop $0 ; return error(1)/success(0)

; Enable ICMP outbound parameter problem
  SimpleFC::AllowDisallowIcmpOutboundParameterProblem 1
  Pop $0 ; return error(1)/success(0)

; Enable ICMP outbound source quench
  SimpleFC::AllowDisallowIcmpOutboundSourceQuench 1
  Pop $0 ; return error(1)/success(0)

; Enable ICMP inbound router request
  SimpleFC::AllowDisallowIcmpInboundRouterRequest 1
  Pop $0 ; return error(1)/success(0)

; Enable ICMP inbound timestamp request
  SimpleFC::AllowDisallowIcmpInboundTimestampRequest 1
  Pop $0 ; return error(1)/success(0)

; Enable ICMP inbound mask request
  SimpleFC::AllowDisallowIcmpInboundMaskRequest 1
  Pop $0 ; return error(1)/success(0)

; Enable ICMP outbound packet too big
  SimpleFC::AllowDisallowIcmpOutboundPacketTooBig 1
  Pop $0 ; return error(1)/success(0)

; Check if ICMPv4 echo request is allowed
  SimpleFC::IsIcmpTypeAllowed "0" "" "8"
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=Restricted/0=Not restricted
  Pop $2 ; return 1=Allowed/0=Not allowed
```

#### Windows Firewall with Advanced Security

Some example rules for the Windows Firewall with Advanced Security. Please note
these functions are very powerful, so for a detailed description please read the
[Windows Firewall with Advanced Security API reference](https://learn.microsoft.com/en-us/previous-versions/windows/desktop/ics/windows-firewall-with-advanced-security-reference).

```nsis
; Adds an ICMPv4 rule to allow incoming echo reply messages (IcmpCodeAndType = 0:0)
  SimpleFC::AdvAddRule "Echo-Reply (ICMPv4 incoming)" "Allows incoming Echo Replies messages." "1" "1" "1" "7" "1" "" "" "0:0" "@PathToApplication,-10000" "" "" "" ""
  Pop $0 ; return error(1)/success(0)

; Adds an ICMPv4 rule to allow incoming echo request messages (IcmpCodeAndType = 8:0)
  SimpleFC::AdvAddRule "Echo-Request (ICMPv4 incoming)" "Allows incoming ICMP Echo messages." "1" "1" "1" "7" "1" "" "" "8:0" "@PathToApplication,-10000" "" "" "" ""
  Pop $0 ; return error(1)/success(0)

; Add an application rule to allow incoming TCP access on this application
  SimpleFC::AdvAddRule "Incoming requests (TCP incoming)" "Allows incoming requests." "6" "1" "1" "7" "1" "PathToApplication" "" "" "@PathToApplication,-10000" "" "" "" ""
  Pop $0 ; return error(1)/success(0)

; Add an application rule to allow incoming UDP access on this application
  SimpleFC::AdvAddRule "Incoming requests (UDP incoming)" "Allows incoming requests." "17" "1" "1" "7" "1" "PathToApplication" "" "" "@PathToApplication,-10000" "" "" "" ""
  Pop $0 ; return error(1)/success(0)

; Removes a firewall rule
  SimpleFC::AdvRemoveRule "Incoming requests (UDP incoming)"
  Pop $0 ; return error(1)/success(0)

; Check if the firewall rule exists
  SimpleFC::AdvExistsRule "Incoming requests (UDP incoming)"
  Pop $0 ; return error(1)/success(0)
  Pop $1 ; return 1=Exists/0=Doesn't exist
```

## Important Notes

- It is recommended to check if the Windows Firewall service is running (`SimpleFC::IsFirewallServiceRunning`).
- All functions with the prefix `Adv` are only for the Windows Firewall with Advanced Security (Windows Vista and above). It is recommended to use these functions on operating systems which support the Windows Firewall with Advanced Security. Nevertheless, the default functions without the prefix `Adv` can be used.

## Building from Source

The ANSI and Unicode variants are separate code bases.

| Variant | Folder | Project | Created with |
| --- | --- | --- | --- |
| ANSI | [`src/ansi`](src/ansi) | `SimpleFC.dpr` | Delphi 6 |
| Unicode | [`src/unicode`](src/unicode) | `SimpleFC.dproj` | Delphi 10.3 Rio |

Both variants build a 32-bit `SimpleFC.dll`.

## License

SimpleFC is dual-licensed under the Mozilla Public License 1.1 or the GNU Lesser
General Public License 2.1 or later. See [License.txt](License.txt) for details.
