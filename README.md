# Covid-19 exposure app

## Setup

To access the private GitHubPackages repository that hosts the remote configurations for all environments, two properties have to be set in your global gradle.properties file:

  *  **gpr.user** is your Github username
  *  **gpr.key**  is a personal access token [issued by GitHub](https://github.com/settings/tokens). For the scope, use at least read:packages and write:packages.

Having these properties in place is necessary to build the app. For more information, visit the [app configuration repository](https://github.com/nhsx/covid-19-app-configuration-public/packages).

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
