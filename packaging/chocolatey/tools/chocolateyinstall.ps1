$ErrorActionPreference = 'Stop'; # stop on all errors

$toolsDir   = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$fileLocation = Join-Path $toolsDir 'Spotube-windows-x86_64-setup.exe'

$packageArgs = @{
  packageName   = $env:ChocolateyPackageName
  fileType      = 'exe'
  file          = $fileLocation

  softwareName  = 'Spotube*'
  silentArgs    = '/S' # NSIS (electron-builder one-click, per-user)
  validExitCodes= @(0)
}

Install-ChocolateyInstallPackage @packageArgs

# The installer was only needed to install; don't keep a copy in the package directory
Remove-Item $fileLocation -Force -ErrorAction SilentlyContinue
