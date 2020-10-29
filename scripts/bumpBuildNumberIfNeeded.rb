if system("git describe --exact-match --tags HEAD") 
    puts "Tag found, skipping version bump."
elsif not ARGV.empty?
    puts "No tag found. Bump buildnumber, create tag and push."
    VERSION_NAME_PATH = "app/versionName"
    version_name = File.open(VERSION_NAME_PATH).read.strip
    BUILD_NUMBER_PATH = "app/buildNumber"
    build_number = File.open(BUILD_NUMBER_PATH).read.to_i
    incremented_build_number = build_number + 1
    File.write(BUILD_NUMBER_PATH, incremented_build_number)
    puts "Changed buildNumber to #{incremented_build_number}"

    system("git add #{BUILD_NUMBER_PATH}")
    system("git commit -m \"Update build number to #{incremented_build_number}\"")
    system("git tag production-v#{version_name}-#{incremented_build_number}")
    system("git push origin #{ARGV[0]} --tags")
else
    puts "No branch provided"
end
