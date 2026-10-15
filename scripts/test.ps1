$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$output = Join-Path $projectRoot 'build\policy-tests'
New-Item -ItemType Directory -Force -Path $output | Out-Null
& javac -encoding UTF-8 -d $output (Join-Path $projectRoot 'app\src\main\java\vn\quietcall\ScreeningEngine.java') (Join-Path $projectRoot 'app\src\main\java\vn\quietcall\DirectoryCsv.java') (Join-Path $projectRoot 'app\src\main\java\vn\quietcall\SpamReport.java') (Join-Path $projectRoot 'app\src\main\java\vn\quietcall\BrandSearch.java') (Join-Path $projectRoot 'tests\ScreeningEngineTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& java -cp $output vn.quietcall.ScreeningEngineTest
if ($LASTEXITCODE -ne 0) { throw 'Policy tests failed' }
