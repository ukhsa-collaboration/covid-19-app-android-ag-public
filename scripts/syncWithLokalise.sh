#!/bin/sh

# Generate and upload zip for newest translations
echo "Requesting translations..."
PROJECT_ID=${1}
LOKALISE_BRANCH=${2}
API_TOKEN=${3}

response=$(curl --request POST \
  --url "https://api.lokalise.com/api2/projects/${PROJECT_ID}:${LOKALISE_BRANCH}/files/download" \
  --header 'content-type: application/json' \
  --header "x-api-token: ${API_TOKEN}" \
  --data '{"format":"xml","original_filenames":false,"indentation":"tab"}')

# Clean link to generated zip
bundle_url=$(echo ${response} | sed 's/\\//g' | grep -oiE '\"https.*"' | sed 's/"//g')

# Download translations
echo "Downloading translations..."
curl ${bundle_url} -LO

echo "Unpacking..."
unzip NHS_COVID-19-strings.zip

echo "Running StringSorter..."
kotlinc -script StringSorter.kts

echo "Cleaning up..."
rm -r locale
rm NHS_COVID-19-strings.zip
