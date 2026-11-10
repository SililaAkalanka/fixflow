# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MvnArgs
)

$ErrorActionPreference = 'Stop'
$WrapperDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent (Split-Path -Parent $WrapperDir)
if (-not $ProjectDir) {
    $ProjectDir = (Get-Location).Path
}

$PropertiesFile = Join-Path $WrapperDir "maven-wrapper.properties"
if (-not (Test-Path $PropertiesFile)) {
    Write-Error "Could not find maven-wrapper.properties at $PropertiesFile"
    exit 1
}

$Properties = @{}
Get-Content $PropertiesFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $parts = $line.Split("=", 2)
        $Properties[$parts[0].Trim()] = $parts[1].Trim()
    }
}

$DistributionUrl = $Properties["distributionUrl"]
if (-not $DistributionUrl) {
    Write-Error "distributionUrl is not configured in $PropertiesFile"
    exit 1
}

$DistName = [System.IO.Path]::GetFileNameWithoutExtension($DistributionUrl)
$ZipName = [System.IO.Path]::GetFileName($DistributionUrl)

$UserHome = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { [System.Environment]::GetFolderPath([System.Environment+SpecialFolder]::UserProfile) }
$MavenUserHome = Join-Path $UserHome ".m2\wrapper\dists"
$DistDir = Join-Path $MavenUserHome "$DistName"

# Locate mvn.cmd
$MvnCmd = $null
if (Test-Path $DistDir) {
    $ExistingMvn = Get-ChildItem -Path $DistDir -Filter "mvn.cmd" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($ExistingMvn) {
        $MvnCmd = $ExistingMvn.FullName
    }
}

if (-not $MvnCmd) {
    Write-Host "Maven distribution not found locally. Downloading from: $DistributionUrl"
    New-Item -ItemType Directory -Path $DistDir -Force | Out-Null
    $ZipPath = Join-Path $DistDir $ZipName

    try {
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12 -bor [System.Net.SecurityProtocolType]::Tls13
        $WebClient = New-Object System.Net.WebClient
        $WebClient.DownloadFile($DistributionUrl, $ZipPath)
    }
    catch {
        Write-Error "Failed to download Maven distribution from $($DistributionUrl): $_"
        exit 1
    }

    Write-Host "Extracting Maven distribution to: $DistDir"
    Expand-Archive -Path $ZipPath -DestinationPath $DistDir -Force
    Remove-Item -Path $ZipPath -Force

    $ExistingMvn = Get-ChildItem -Path $DistDir -Filter "mvn.cmd" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($ExistingMvn) {
        $MvnCmd = $ExistingMvn.FullName
    } else {
        Write-Error "Could not locate mvn.cmd inside $DistDir after extraction."
        exit 1
    }
}

if ($MvnArgs) {
    & "$MvnCmd" @MvnArgs
} else {
    & "$MvnCmd"
}
exit $LASTEXITCODE
