Var GoCDCurrentVersion
Var IsUpgrading

Function "ExitInstaller"
  Quit
FunctionEnd

Function "MaybePerformUpgrade"
  ReadRegStr $1 HKLM "Software\ThoughtWorks Studios\Go ${COMPONENT_NAME}" "Ver"

  ${If} $1 L= ${COMPONENT_REGISTRY_VERSION}
      Call UpgradeToSameVersionMessageAndExit
  ${ElseIf} $1 L< ${COMPONENT_REGISTRY_VERSION}
      Call UpgradeToNewerVersionAndContinue
  ${Else}
      Call UpgradeToOlderVersionMessageAndExit
  ${EndIf}
FunctionEnd

Function "UpgradeToNewerVersionAndContinue"
  ReadRegStr $GoCDCurrentVersion HKLM "Software\ThoughtWorks Studios\Go ${COMPONENT_NAME}" "Version"

  ${LogText} "Go ${COMPONENT_NAME} upgraded from $GoCDCurrentVersion to ${COMPONENT_FULL_VERSION}"

  ${IfNot} ${Silent}
    MessageBox MB_YESNO \
        "This will upgrade Go ${COMPONENT_NAME} from $GoCDCurrentVersion to ${COMPONENT_FULL_VERSION}.$\r$\nMake sure you have backups before doing this!$\r$\nDo you want to continue?" \
        IDYES continueUpgradeLabel \
        IDNO exitInstallerLabel
  ${EndIf}

  continueUpgradeLabel:
    ${LogText} "Requested to continue installing."
    StrCpy $IsUpgrading "true"
    Return

  exitInstallerLabel:
    ${LogText} "Requested to exit installer."
    Call ExitInstaller
    Return
FunctionEnd

Function "UpgradeToSameVersionMessageAndExit"
  ${LogText} "Go ${COMPONENT_NAME} ${COMPONENT_FULL_VERSION} is already installed."

  ${IfNot} ${Silent}
    MessageBox MB_OK "Go ${COMPONENT_NAME} ${COMPONENT_FULL_VERSION} is already installed."
  ${EndIf}

  Call ExitInstaller
FunctionEnd

Function "UpgradeToOlderVersionMessageAndExit"
  ReadRegStr $GoCDCurrentVersion HKLM "Software\ThoughtWorks Studios\Go ${COMPONENT_NAME}" "Version"
  ${LogText} "Go ${COMPONENT_NAME} $GoCDCurrentVersion is installed, and you are trying to install an older version (${COMPONENT_FULL_VERSION}). This is not supported."


  ${IfNot} ${Silent}
    MessageBox MB_OK "Go ${COMPONENT_NAME} $GoCDCurrentVersion is installed, and you are trying to install an older version (${COMPONENT_FULL_VERSION}).$\r$\nThis is not supported."
  ${EndIf}
FunctionEnd
