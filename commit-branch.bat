@echo off
setlocal

cd /d "%~dp0"

set "BRANCH_NAME=%~1"
set "COMMIT_MSG=%~2"

if not defined BRANCH_NAME goto missing_arguments
if not defined COMMIT_MSG goto missing_arguments

git checkout -b "%BRANCH_NAME%"
if errorlevel 1 exit /b 1

git add java/src
if errorlevel 1 exit /b 1

git commit -m "%COMMIT_MSG%"
if errorlevel 1 exit /b 1

git fetch origin
if errorlevel 1 exit /b 1

git checkout main
if errorlevel 1 exit /b 1

git pull origin main
if errorlevel 1 exit /b 1

echo Merging branch into main: %BRANCH_NAME%...
git merge --no-ff "%BRANCH_NAME%"
if errorlevel 1 goto merge_failed

echo Merge successful. The branch is now merged into local main.
exit /b 0

:merge_failed
echo Merge failed. Current status:
git status --short
git merge --abort
if errorlevel 1 echo Could not abort the failed merge. Resolve the merge state manually.
exit /b 1

:missing_arguments
echo Error: Missing arguments.
echo Usage: %~nx0 ^<branch_name^> ^<commit_message^>
exit /b 1
