$ErrorActionPreference = 'Stop'; # stop on all errors
$packageArgs = @{
  packageName   = $env:ChocolateyPackageName
  softwareName  = 'Spotube*'
  fileType      = 'exe'
  validExitCodes= @(0)
}

[array]$key = Get-UninstallRegistryKey -SoftwareName $packageArgs['softwareName']

if ($key.Count -eq 1) {
  $key | % {
    # electron-builder writes UninstallString as "<dir>\Uninstall Spotube.exe" /currentuser
    if ($_.UninstallString -match '^\s*"([^"]+)"\s*(.*)$') {
      $packageArgs['file'] = $Matches[1]
      $packageArgs['silentArgs'] = "$($Matches[2]) /S".Trim()
    } else {
      $packageArgs['file'] = $_.UninstallString
      $packageArgs['silentArgs'] = '/S'
    }

    Uninstall-ChocolateyPackage @packageArgs
  }
} elseif ($key.Count -eq 0) {
  Write-Warning "$packageName has already been uninstalled by other means."
} elseif ($key.Count -gt 1) {
  Write-Warning "$($key.Count) matches found!"
  Write-Warning "To prevent accidental data loss, no programs will be uninstalled."
  Write-Warning "Please alert package maintainer the following keys were matched:"
  $key | % {Write-Warning "- $($_.DisplayName)"}
}
