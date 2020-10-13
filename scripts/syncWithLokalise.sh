#!/bin/sh

# Generate and upload zip for newest translations
echo "Requesting translations..."
response=$(curl --request POST \
  --url "https://api.lokalise.com/api2/projects/$1:$2/files/download" \
  --header 'content-type: application/json' \
  --header "x-api-token: $3" \
  --data '{"format":"xml","original_filenames":false,"indentation":"tab"}')

# Clean link to generated zip
bundle_url=$(echo $response | sed 's/\\//g' | grep -oiE '\"https.*"' | sed 's/"//g')

# Download translations
echo "Downloading translations..."
curl $bundle_url -LO

echo "Unpacking..."
unzip NHS_COVID-19-strings.zip

echo "Compiling StringSorter..."
kotlinc StringSorter.kt -include-runtime -d StringSorter.jar

echo "Sorting..."
java -jar StringSorter.jar

echo "Cleaning up..."
rm -r locale
rm NHS_COVID-19-strings.zip
rm StringSorter.jar
