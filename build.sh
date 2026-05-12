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

# Create specialized jars for training dummy champions and their related classes
echo "📦 Creating training dummy plugin JARs..."
jar cf trainingdummy.jar \
	-C target TrainingDummy.class \
	-C src TrainingDummy.png \
	-C target Headbutt.class \
	-C target Shrug.class \
	-C target Bandaid.class \
	-C target EmberCrystal.class \
	-C target Chargebreaker.class \
	-C target RecklessBurst.class \
    -C target DefenseDrop.class

jar cf advancedtrainingdummy.jar \
	-C target AdvancedTrainingDummy.class \
	-C src AdvancedTrainingDummy.png \
	-C target Jab.class \
	-C target Brace.class \
	-C target WindUpSlam.class \
	-C target PoisonDart.class \
	-C target BraceBuff.class \
	-C target PoisonEffect.class \
	-C target StoneAmulet.class \
	-C target LastLight.class \
	-C target AdrenalSurge.class \

# Creating javaDoc
echo "📜 Generating JavaDoc..."
javadoc -d docs src/*.java
zip -r docs.zip docs

echo "✅ Build complete: champions-arena.jar, trainingdummy.jar, advancedtrainingdummy.jar"
