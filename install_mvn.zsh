#!/bin/zsh

VERSION="3.9.6"
MAVEN_DIR="$HOME/apache-maven-$VERSION"
ARCHIVE_URL="https://archive.apache.org/dist/maven/maven-3/$VERSION/binaries/apache-maven-$VERSION-bin.tar.gz"

echo "Downloading Maven $VERSION..."
wget -O /tmp/maven.tar.gz "$ARCHIVE_URL" || {
  echo "Download failed ❌"
  exit 1
}

echo "Extracting..."
tar -xzf /tmp/maven.tar.gz -C "$HOME" || exit 1

echo "Updating .zshrc ..."
if ! grep -q "MAVEN_HOME=.*apache-maven-$VERSION" ~/.zshrc; then
  {
    echo ""
    echo "# Maven $VERSION"
    echo "export MAVEN_HOME=\"$MAVEN_DIR\""
    echo "export PATH=\"\$MAVEN_HOME/bin:\$PATH\""
  } >> ~/.zshrc
fi

echo "Reloading shell..."
source ~/.zshrc

echo "Done ✅"
echo "Run: mvn -v"