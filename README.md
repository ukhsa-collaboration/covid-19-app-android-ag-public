# Covid-19 exposure app

## Setup

To access the private GitHubPackages repository that hosts the remote configurations for all environments, your Github username has to be set in your global gradle.properties file:

  *  **gpr.user** is your Github username

Furthermore, you have to create a personal access token [issued by GitHub](https://github.com/settings/tokens). For the scope, use at least read:packages and write:packages.
This token has to be stored in the Keychain for MacOS users or the Credential Manager for Windows users.

### Add your personal access token to the Keychain (MacOS)

1. Open Keychain Access
2. Go to File > New Password Item…
   1. __Service__ (__Keychain Item Name__) should be _nihp-public_
   2. __Account__ is your Github username
   3. As __Password__ enter your personal access token

### Add your personal access token to the Credential Manager (Windows)

First the token has to be stored.

1. Open Credential Manager
2. Go to _Add a generic credential_
   1. __Internet or work address__ should be _nihp-public_
   2. __User name__ is your Github username
   3. As __Password__ enter your personal access token

To enable PowerShell to read from the Credential Manager, the CredentialManager module needs to be installed.

1. Open a PowerShell instance with Administrator privileges
2. Enter _Install-Module -Name CredentialManager_
3. (optional) Troubleshooting
   1. _Install-Module_ uses NuGet. If NuGet is not pre-installed, install it with _Install-PackageProvider -Name NuGet_ from PowerShell
   2. Installing NuGet might fail due to inappropriate TLS settings. Enforcing TLS 1.2 to install NuGet fixes this potential issue: Enter _\[Net.ServicePointManager]::SecurityProtocol = \[Net.SecurityProtocolType]::Tls12_ into PowerShell to enforce usage of TLS 1.2

### Optional: Setup path to isolation model in global gradle.properties

In order to get a direct link to the related isolation rule in case of a failing isolation test, you need to add the path to the repo hosting the rules in the global gradle.properties file:

  * `isolationModel.repo = https://<insert path to repo here>`

## Dev actions

1. Device setup

    Make sure all animations are turned off on the device (not an emulator) that is running the tests.
    https://developer.android.com/training/testing/espresso/setup#set-up-environment 

1. Run linter:
   ```bash
   ./gradlew ktlintFormat
   ```
   
1. Run build and tests:
   ```bash
   ./gradlew
   ```
