# Clean build output
Write-Host "Cleaning build output..."
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue target
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue docs
New-Item -ItemType Directory -Force target | Out-Null
New-Item -ItemType Directory -Force docs | Out-Null

function Resolve-JdkToolPath {
	param (
		[Parameter(Mandatory = $true)]
		[string]$ToolName
	)

	$candidates = @()

	$cmdTool = Get-Command $ToolName -ErrorAction SilentlyContinue
	if ($cmdTool) {
		$candidates += $cmdTool.Source
	}

	if ($env:JAVA_HOME) {
		$candidates += Join-Path $env:JAVA_HOME ("bin\" + $ToolName + ".exe")
	}

	$cmdJavac = Get-Command javac -ErrorAction SilentlyContinue
	if ($cmdJavac) {
		$candidates += Join-Path (Split-Path $cmdJavac.Source -Parent) ($ToolName + ".exe")
	}

	$searchRoots = @(
		"C:\Program Files\Java",
		"C:\Program Files\Eclipse Adoptium",
		"C:\Program Files\Zulu",
		"C:\Program Files\Amazon Corretto",
		"C:\Program Files\Microsoft"
	)

	foreach ($root in $searchRoots) {
		if (Test-Path $root) {
			$found = Get-ChildItem -Path $root -Filter ($ToolName + ".exe") -Recurse -File -ErrorAction SilentlyContinue | Select-Object -First 1
			if ($found) {
				$candidates += $found.FullName
			}
		}
	}

	$resolved = $candidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
	return $resolved
}

# Compile all Java source files into target/
Write-Host "Compiling Java source files..."
$javacPath = Resolve-JdkToolPath -ToolName "javac"
if (-not $javacPath) {
	throw "Could not find javac.exe. Set JAVA_HOME to a full JDK install."
}
$javaFiles = Get-ChildItem -Path src -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& $javacPath -encoding UTF-8 -source 8 -target 8 -d target $javaFiles -Xlint:-options
if ($LASTEXITCODE -ne 0) {
	throw "Java compilation failed."
}

# Create manifest file with Main-Class
Write-Host "Creating manifest file..."
"Main-Class: ChampionsArena" | Set-Content target/MANIFEST.MF

# Create jar from compiled classes + manifest
Write-Host "Creating JAR file..."
$jarPath = Resolve-JdkToolPath -ToolName "jar"
if (-not $jarPath) {
	throw "Could not find jar.exe. Set JAVA_HOME to a full JDK install."
}
& $jarPath cfm champions-arena.jar target/MANIFEST.MF -C target .
if ($LASTEXITCODE -ne 0) {
	throw "JAR creation failed."
}

# Generate JavaDoc when available
Write-Host "Generating JavaDoc..."
$srcFiles = Get-ChildItem -Path src -Filter "*.java" -File | Select-Object -ExpandProperty FullName
$javaDocPath = Resolve-JdkToolPath -ToolName "javadoc"

if (-not $javaDocPath) {
	Write-Warning "Could not find javadoc.exe. Skipping JavaDoc generation. Set JAVA_HOME to a full JDK install to enable docs."
}
elseif (-not $srcFiles) {
	Write-Warning "No Java source files found for JavaDoc generation."
}
else {
	& $javaDocPath -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -d docs $srcFiles
	if ($LASTEXITCODE -ne 0) {
		throw "JavaDoc generation failed."
	}
	Compress-Archive -Path docs -DestinationPath docs.zip -Force
}

Write-Host "Build complete: champions-arena.jar"
