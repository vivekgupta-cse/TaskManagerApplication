#!/bin/bash -x
URL='http://localhost:9090'
HEADER='Content-Type: application/json'

# Source external files containing user and task data
source Users.sh
source Tasks.sh

# --- 1. Register Users ---
echo "--- Registering Users ---"
for (( i=0; i<${#USERS[@]}; i+=2 )); do
    USERNAME=${USERS[i]}
    PASSWORD=${USERS[i+1]}

    echo "Registering user: $USERNAME"
    # Use a "here document" for cleaner JSON
    curl --location "${URL}/api/auth/register" --header "${HEADER}" --data @- <<EOF
{
  "username": "$USERNAME",
  "password": "$PASSWORD"
}
EOF
    echo ""
done

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

# --- 3. Create Tasks Using the Token ---
echo "--- Creating Tasks ---"
AUTH_HEADER="Authorization: Bearer $TOKEN"

for (( i=0; i<${#TASKS[@]}; i+=3 )); do
    TITLE=${TASKS[i]}
    DESCRIPTION=${TASKS[i+1]}
    COMPLETED=${TASKS[i+2]}

    echo "Creating task: '$TITLE'"
    curl --location "${URL}/api/tasks" --header "${AUTH_HEADER}" --header "${HEADER}" --data @- <<EOF
{
  "title": "$TITLE",
  "description": "$DESCRIPTION",
  "completed": $COMPLETED
}
EOF
    echo ""
done

# --- 4. Search a task Using the Token ---
echo "--- Searching Tasks ---"
AUTH_HEADER="Authorization: Bearer $TOKEN"

RESPONSE=$(curl --location "${URL}/api/tasks/search?title=Grocery" --header "${AUTH_HEADER}" --header "${HEADER}")
TASKID=$(jq -r '.content[0].id' <<< $RESPONSE)


# --- 5. Update Tasks Using the Token ---
echo "--- Updating Tasks ---"
AUTH_HEADER="Authorization: Bearer $TOKEN"

curl --location --request PUT "${URL}/api/tasks/${TASKID}" --header "${AUTH_HEADER}" --header "${HEADER}" --data @- <<EOF
{
  "title": "Buy Groceries",
  "description": "Dal, Chawal, Ghee, Butter, Samosa",
  "completed": true
}
EOF


# --- 6. Delete tasks matching a criteria ---
echo "--- Deleting Tasks ---"
AUTH_HEADER="Authorization: Bearer $TOKEN"

echo "--- searching first ---"
RESPONSE=$(curl --location "${URL}/api/tasks/search?title=in" --header "${AUTH_HEADER}" --header "${HEADER}")
mapfile -t TASK_IDS < <(echo "$RESPONSE" | jq -r '.content[].id')
echo "Found ${#TASK_IDS[@]} tasks to delete."

for (( i=0; i<${#TASK_IDS[@]}; i+=1 )); do
    TASK_ID=${TASK_IDS[i]}
    curl --location --request DELETE "${URL}/api/tasks/{$TASK_ID}" --header "${AUTH_HEADER}"
done

echo "--- Script Finished ---"
