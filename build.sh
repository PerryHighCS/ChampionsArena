#!/bin/bash

set -euo pipefail

# Clean build output
echo "🧹 Cleaning build output..."
rm -rf target
rm -rf docs
mkdir -p target
mkdir -p docs

# Compile all Java source files into target/
echo "🛠️  Compiling Java source files..."
javac -source 8 -target 8 -d target $(find src -name "*.java") -Xlint:-options

# Create manifest file with Main-Class
echo "📝 Creating manifest file..."
echo "Main-Class: ChampionsArena" > target/MANIFEST.MF

# Create jar from compiled classes + manifest
echo "📦 Creating JAR file..."
jar cfm champions-arena.jar target/MANIFEST.MF -C target .

# Creating javaDoc
echo "📜 Generating JavaDoc..."
javadoc -d docs src/*.java
zip -r docs.zip docs

echo "✅ Build complete: champions-arena.jar"
