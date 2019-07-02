!include "go-base.nsi"
!include "gocd-server-firewall-helpers.nsi"

Function "OnInitCallback"
  ; do nothing
FunctionEnd

Function "un.PostUninstall"
  Call "un.DisableFirewallPorts"

  Delete "$DESKTOP\Go ${COMPONENT_NAME}.url"

  ; surgically remove only some files, leave the DB, config etc around
  RMDir /r "$INSTDIR\bin"
  RMDir /r "$INSTDIR\jre"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\logs"
  RMDir /r "$INSTDIR\run"
  RMDir /r "$INSTDIR\tanuki"
  RMDir /r "$INSTDIR\tmp"
  RMDir /r "$INSTDIR\work"
FunctionEnd

Function "BeforeUpgrade"
  Call RememberIfServiceWasRunning
  Call StopServiceIfRunning
  ; this will uninstall the old service, and install a new one, just to make sure that changes to tanuki, if any happen correctly
  Call UninstallService

  ; remove legacy files (from pre 19.5 installers, if present)
  Delete "$INSTDIR\jre" ; the old jre
  Delete "$INSTDIR\config\wrapper-server.conf"
  Delete "$INSTDIR\cruise.war"
  Delete "$INSTDIR\go.jar"
  Delete "$INSTDIR\LICENSE.dos"
  Delete "$INSTDIR\cruisewrapper.exe"
  Delete "$INSTDIR\server.cmd"
  Delete "$INSTDIR\start-server.bat"
  Delete "$INSTDIR\stop-server.bat"

  ; the jetty work dir
  RMDir /r "$INSTDIR\work"

  ; remove legacy environment variables (from pre 19.5 installers), if present
  DeleteRegValue HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "GO_SERVER_JAVA_HOME"
  DeleteRegValue HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "CRUISE_SERVER_DIR"

  ; explicitly clear errors, if any
  ClearErrors
FunctionEnd

Function "AfterUpgrade"
  Call InstallService
  Call StartServiceIfRunningBeforeUpgrade
FunctionEnd

Function "PostInstall"
  ${LogText} "Postinstall"
  Call "OpenFirewallPorts"
  Call "CreateServerUrlShortcut"
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
