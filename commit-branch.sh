#!/bin/bash
set -e

cd "$(dirname "$0")"

BRANCH_NAME="$1"
COMMIT_MSG="$2"

if [ -z "$BRANCH_NAME" ] || [ -z "$COMMIT_MSG" ]; then
    echo "Error: Missing arguments."
    echo "Usage: ./commit-branch.sh <branch_name> <commit_message>"
    exit 1
fi

git checkout -b "$BRANCH_NAME"
git add java/src
git commit -m "$COMMIT_MSG"

git fetch origin
git checkout main
git pull origin main

echo "Merging branch into main: $BRANCH_NAME..."

if git merge --no-ff "$BRANCH_NAME"; then
    git push origin main
    echo "Merge successful."
    TITLE="Merge Successful"
    MESSAGE="Merge successful."
    osascript -e "display notification \"$MESSAGE\" with title \"$TITLE\" sound name \"Glass\""
else
    echo "Merge failed."
    git status --short
    git merge --abort
    TITLE="Merge Failed"
    MESSAGE="sad :("
    osascript -e "display notification \"$MESSAGE\" with title \"$TITLE\" sound name \"Basso\""
    exit 1
fi
