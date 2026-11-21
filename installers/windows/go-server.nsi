!include "go-base.nsi"
!include "gocd-server-firewall-helpers.nsi"

Function "OnInitCallback"
  ; do nothing
FunctionEnd

; Working data which the server (re)generates as needed. Excludes logs, config, artifacts, DB etc. Not needed on upgrade,
; since the server replaces or cleans these itself on startup.
Function "un.RemoveServerWorkingData"
  ${LogText} "Removing server working data..."
  Delete "$INSTDIR\cruise.war"
  RMDir /r "$INSTDIR\felix-cache"
  Delete "$INSTDIR\plugins\go-plugins-all.zip"
  RMDir /r "$INSTDIR\plugins\bundled"
  RMDir /r "$INSTDIR\plugins_work"
  RMDir /r "$INSTDIR\run"
  RMDir /r "$INSTDIR\tmp"
  RMDir /r "$INSTDIR\work"
  ; explicitly clear errors, if any
  ClearErrors
FunctionEnd

!insertmacro RemoveInstallerManagedFiles "un."

Function "un.PostUninstall"
  Call "un.DisableFirewallPorts"

  Delete "$DESKTOP\Go ${COMPONENT_NAME}.url"

  Call "un.RemoveInstallerManagedFiles"
  Call "un.RemoveServerWorkingData"
  RMDir /r "$INSTDIR\logs"
FunctionEnd

Function "BeforeUpgrade"
  Call RememberIfServiceWasRunning
  Call StopServiceIfRunning
  ; this will uninstall the old service, and install a new one, just to make sure that changes to tanuki, if any happen correctly
  Call UninstallService

  Call RemoveInstallerManagedFiles
FunctionEnd

Function "AfterUpgrade"
  Call InstallService
  Call StartServiceIfRunningBeforeUpgrade
FunctionEnd

Function "PostInstall"
  ${LogText} "Postinstall"
  Call "OpenFirewallPorts"
  Call "CreateServerUrlShortcut"
  Call InstallService
  Call StartService
FunctionEnd

Function "DoParseCLI"
  ; do nothing, we don't support any CLI args for the silent installer
FunctionEnd

Function "CreateServerUrlShortcut"
  FileOpen $0 "$SMPROGRAMS\Go Server\Go Server.url" w
  ${IfNot} ${Errors}
    FileWrite $0 "[InternetShortcut]$\r$\n"
    FileWrite $0 "URL=http://localhost:8153/go$\r$\n"
    FileWrite $0 "IDList=$\r$\n"
    FileWrite $0 "IconFile=$INSTDIR\gocd.ico$\r$\n"
    FileWrite $0 "IconIndex=0$\r$\n"
    FileClose $0
  ${EndIf}
FunctionEnd
