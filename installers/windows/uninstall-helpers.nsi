Function "un.onInit"
  ; Do not do this in the "uninstall" section. Save yourselves some hours :)
  ; Moving this to "Uninstall" section will cause logging to be disabled for fresh install scenarios
  SetOutPath $INSTDIR
  ${LogSet} on
FunctionEnd

; NSIS recognizes `Uninstall` as a special section to be used for the uninstallation procedure
Section "Uninstall"
  ; remember the name of the service in a variable
  StrCpy $GoCDServiceName "Go ${COMPONENT_NAME}"

  ; This needs to be prefixed with `un.`
  Call "un.RememberIfServiceWasRunning"
  Call "un.StopServiceIfRunning"
  Call "un.UninstallService"

  Call "un.RemoveStartMenuEntriesAndUninstaller"
  !include "${UNINSTALL_MANIFEST_FILE}"
  Call "un.PostUninstall"
  Call "un.RemoveRegistryKeys"
SectionEnd

Function "un.RemoveStartMenuEntriesAndUninstaller"
  RMDir /r "$SMPROGRAMS\Go ${COMPONENT_NAME}"
  Delete "$INSTDIR\uninstall.exe"
FunctionEnd

Function "un.RemoveRegistryKeys"
  ; remove the entire metadata about this component
  DeleteRegKey HKLM "SOFTWARE\ThoughtWorks Studios\Go ${COMPONENT_NAME}"

  ; Remove the uninstall keys for Windows
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Go ${COMPONENT_NAME}"
FunctionEnd
