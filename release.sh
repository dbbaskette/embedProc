#!/bin/bash

set -e

# 1. Increment the patch version in pom.xml
echo "Incrementing patch version in pom.xml..."
current_version=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" pom.xml)
IFS='.' read -r major minor patch <<< "$current_version"
new_patch=$((patch + 1))
new_version="$major.$minor.$new_patch"
echo "Current version: $current_version"
echo "New version: $new_version"
# Update the version in pom.xml
sed -i '' "s/<version>$current_version<\/version>/<version>$new_version<\/version>/" pom.xml

# 2. Prompt for a commit message
read -p "Enter commit message: " commit_msg

# 3. Commit and push to remote
git add pom.xml
git commit -m "$commit_msg"
git push

# 4. Create a new tag and push it
git tag -a "v$new_version" -m "Release v$new_version"
git push origin "v$new_version"

# 5. Create a new GitHub release
gh release create "v$new_version" --title "Release v$new_version" --notes "$commit_msg"

# 6. Build the new jar
mvn clean package -DskipTests

# 7. Upload the jar to the GitHub release
JAR_FILE="target/$(ls target | grep '.*\.jar$' | grep "$new_version" | head -n1)"
if [[ -f "$JAR_FILE" ]]; then
    gh release upload "v$new_version" "$JAR_FILE"
    echo "Uploaded $JAR_FILE to release v$new_version."
else
    echo "JAR file for version $new_version not found in target/. Please check the build."
    exit 1
fi

echo "Release process completed successfully!"
