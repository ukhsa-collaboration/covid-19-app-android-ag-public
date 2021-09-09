if system("git describe --exact-match --tags HEAD") 
    puts "Tag found, skipping version bump."
elsif not ARGV.empty?
    puts "No tag found. Create tag and push."
    VERSION_NAME_PATH = "app/versionName"
    version_name = File.open(VERSION_NAME_PATH).read.strip
    BUILD_NUMBER_PATH = "app/buildNumber"
    build_number = File.open(BUILD_NUMBER_PATH).read.to_i

    system("git tag production-v#{version_name}-#{build_number}")
    system("git push origin #{ARGV[0]} --tags")
else
    puts "No branch provided"
end
