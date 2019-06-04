Var GoCDServiceName
Var ServiceWasRunningBeforeUpgradeOrUninstall

!macro StopServiceIfRunning maybePrefix
  ; function "shared" between uninstaller and installer code.
  ; See https://nsis.sourceforge.io/Sharing_functions_between_Installer_and_Uninstaller
  Function "${maybePrefix}StopServiceIfRunning"
    ${LogText} "Stopping service $GoCDServiceName if running..."
    ${If} $ServiceWasRunningBeforeUpgradeOrUninstall == "true"
      ${LogText} "Stopping service $GoCDServiceName because it was running"
      SimpleSC::StopService $GoCDServiceName 30 30
      Pop $0 ; returns an errorcode (<>0) otherwise success (0)
      ${If} $0 != 0 ;
        ${ShowMessageAndExit} "Unable to stop service $GoCDServiceName (err: $0)!"
      ${EndIf}

      ${LogText} "Sleeping 3 seconds to wait for file locks to be released..."
      Sleep 3000
      ${LogText} "Continuing after sleep..."
    ${ElseIf} $ServiceWasRunningBeforeUpgradeOrUninstall == "false"
      ; do nothing
      ${LogText} "Service $GoCDServiceName was not running"
    ${Else}
      ${ShowMessageAndExit} "Cannot determine if service was running!"
    ${EndIf}
  FunctionEnd
!macroend

!macro RememberIfServiceWasRunning maybePrefix
  ; function "shared" between uninstaller and installer code.
  ; See https://nsis.sourceforge.io/Sharing_functions_between_Installer_and_Uninstaller
  Function "${maybePrefix}RememberIfServiceWasRunning"
    ${LogText} "Remembering if service $GoCDServiceName is running"
    SimpleSC::ServiceIsRunning $GoCDServiceName
    Pop $0 ; returns an errorcode (<>0) otherwise success (0)
    Pop $1 ; returns 1 (service is running) - returns 0 (service is not running)

    ${If} $1 == 1
      ${LogText} "Service $GoCDServiceName is running"
      StrCpy $ServiceWasRunningBeforeUpgradeOrUninstall "true"
    ${Else}
      ${LogText} "Service $GoCDServiceName is not running"
      StrCpy $ServiceWasRunningBeforeUpgradeOrUninstall "false"
    ${EndIf}
    ClearErrors
  FunctionEnd
!macroend

!insertmacro StopServiceIfRunning ""
!insertmacro StopServiceIfRunning "un."
!insertmacro RememberIfServiceWasRunning ""
!insertmacro RememberIfServiceWasRunning "un."

!macro UninstallService maybePrefix
  Function "${maybePrefix}UninstallService"
    ${LogText} "Removing Service $GoCDServiceName"
    SimpleSC::RemoveService $GoCDServiceName
  FunctionEnd
!macroend

!insertmacro UninstallService ""
!insertmacro UninstallService "un."

Function "StartService"
  ${LogText} "Starting service $GoCDServiceName"
  SimpleSC::StartService $GoCDServiceName "" 30
  Pop $0 ; returns an errorcode (<>0) otherwise success (0)

  ${If} $0 == 0
    ${LogText} "Successfully started service $GoCDServiceName"
  ${Else}
    ${ShowMessageAndExit} "Failed to start service $GoCDServiceName (err: $0)!"
  ${EndIf}
  ClearErrors
FunctionEnd

Function "StartServiceIfRunningBeforeUpgrade"
  ${If} $ServiceWasRunningBeforeUpgradeOrUninstall == "true"
    ${LogText} "Starting service $GoCDServiceName because it was running before upgrade..."
    Call StartService
  ${ElseIf} $ServiceWasRunningBeforeUpgradeOrUninstall == "false"
    ${LogText} "Not starting $GoCDServiceName because it was not running before upgrade..."
    ; do nothing
  ${Else}
    ${ShowMessageAndExit} "Cannot determine if service was running!"
  ${EndIf}
FunctionEnd

Function "InstallService"
  ${LogText} "Installing ${COMPONENT_NAME} service..."
  nsExec::ExecToLog /TIMEOUT=60000 '"$INSTDIR\bin\install-go-${COMPONENT_NAME}-service.bat"'
  Pop $0 ;error code
  ${If} $0 != 0
    ${ShowMessageAndExit} "Failed to install service $GoCDServiceName (err: $0)!"
  ${EndIf}
  ClearErrors
FunctionEnd
