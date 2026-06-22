#!/bin/bash
GREEN="\033[1;32m"
RED="\033[1;31m"
NC="\033[0m"

echo -e "$GREEN\n\n        PROMETHEE WEB SERVER LOAD TEST (Apache Bench) $NC \n"

# Check if ab is installed
if ! command -v ab &> /dev/null
then
    echo -e "$REDError: Apache Bench (ab) is not installed.$NC"
    echo -e "$REDPlease install it using: sudo apt-get install apache2-utils $NC"
    exit 1
fi

# URL of the Promethee Calculation endpoint
# Adjust port if needed
URL="http://localhost:8080/app/calculate"

# JSON payload
PAYLOAD_FILE="test_json/Exemple_Data.json"

if [ ! -f "$PAYLOAD_FILE" ]; then
    echo -e "$RED Error: Payload file $PAYLOAD_FILE not found.$NC"
    exit 1
fi

echo -e "$GREEN\nTarget URL: $URL\n$NC"
echo -e "$GREEN\nPayload: $PAYLOAD_FILE\n$NC"
echo -e "$GREEN\nTesting with 1000 total requests, 100 concurrent requests...\n$NC"

ab -p "$PAYLOAD_FILE" -T "application/json" -c 100 -n 1000 "$URL"

echo -e "$GREEN\nLoad test finished.$NC"
