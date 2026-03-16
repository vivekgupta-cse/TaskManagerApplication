#!/bin/bash -x
URL='http://localhost:9090'
HEADER='Content-Type: application/json'

# --- 2. Login and Capture Token ---
echo "--- Logging in as 'vivek' to get token ---"
# Use -s for silent mode to ensure only the token is captured
TOKEN=$(curl -s --location "${URL}/api/auth/login" --header "${HEADER}" --data @- <<EOF
{
  "username": "vivek",
  "password": "vivek123"
}
EOF
)

if [ -z "$TOKEN" ]; then
    echo "Error: Failed to get token. Exiting."
    exit 1
fi
echo "Token captured successfully."
echo ""

# --- 4. Search a task Using the Token ---
echo "--- Searching Tasks ---"
AUTH_HEADER="Authorization: Bearer $TOKEN"

while true
do
  RESPONSE=$(curl --location "${URL}/api/tasks/search?title=Grocery" --header "${AUTH_HEADER}" --header "${HEADER}")
  TASKID=$(jq -r '.content[0].id' <<< $RESPONSE)
  sleep 1
done

echo "--- Script Finished ---"
