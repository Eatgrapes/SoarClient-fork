#!/bin/bash
# Test script to verify version.json format

BUILD_DATE=$(date -u +"%Y-%m-%d")
BUILD_TIME=$(date -u +"%H:%M:%S")
BUILD_DATETIME=$(date -u +"%Y-%m-%d %H:%M:%S UTC")

# Simulate GitHub environment variables for testing
GITHUB_REPOSITORY="Eatgrapes/SoarClient-fork"
GITHUB_SHA="abc123def456"
GITHUB_REF_NAME="main"

# Generate version.json content
echo "Generating version.json..."
echo "{"build_date":"$BUILD_DATE","build_time":"$BUILD_TIME","build_datetime":"$BUILD_DATETIME","repository":"$GITHUB_REPOSITORY","commit":"$GITHUB_SHA","branch":"$GITHUB_REF_NAME"}" > version.json

echo "Generated version.json content:"
cat version.json

# Validate JSON format
echo "Validating JSON format..."
if command -v jq >/dev/null 2>&1; then
    jq . version.json && echo "✓ Valid JSON"
else
    echo "⚠ jq not found, skipping JSON validation"
fi

echo "File encoding check:"
file -i version.json

echo "Test completed!"
