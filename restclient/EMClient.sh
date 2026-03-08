#!/bin/bash

# Constants
BASE_URL="http://172.32.0.11:32768/api"
GET_COMMANDS_URL="$BASE_URL/get"
CMD_COMMANDS_URL="$BASE_URL/cmd"
COMMANDS_PER_PAGE=5

# Clear the terminal screen
clear_screen() {
    clear
}

echostd() {
    echo "$@" 1>&2
}

debug() {
    echostd "$@"
}

debug_wait() {
    echostd "$@"
    read -p "Press Enter to continue..."
}

# Fetch the entire JSON from the API
fetch_json() {
    response=$(curl -s "$GET_COMMANDS_URL")
    echo "$response"
}

# Get list of available apps (returns array of app objects)
get_app_list() {
    local json="$1"
    echo "$json" | jq -c '.apps[]'
}

# Get app ID by index
get_app_id_by_index() {
    local json="$1"
    local index="$2"
    echo "$json" | jq -r ".apps[$index].id"
}

# Get app description by index
get_app_description_by_index() {
    local json="$1"
    local index="$2"
    echo "$json" | jq -r ".apps[$index].description"
}

# Get commands for a specific app by index
get_app_commands_by_index() {
    local json="$1"
    local index="$2"
    echo "$json" | jq -r ".apps[$index].commands[] | @base64"
}

# Get total number of apps
get_app_count() {
    local json="$1"
    echo "$json" | jq '.apps | length'
}

# Decode commands from base64
decode_command() {
    echo "$1" | base64 -d 2>/dev/null | jq -r '.'
}

# Validate user input
is_valid() {
    local input_value="$1"
    local arg_type="$2"

    case $arg_type in
        "str")
            [[ -n "$input_value" ]]
            ;;
        "int")
            [[ "$input_value" =~ ^-?[0-9]+$ ]]
            ;;
        "unsigned")
            [[ "$input_value" =~ ^[1-9][0-9]*$ ]]
            ;;
        "date")
            date -d "$input_value" "+%Y-%m-%d" &> /dev/null
            ;;
        "time")
            date -d "$input_value" "+%H:%M" &> /dev/null
            ;;
        "duration")
            if [[ "$input_value" =~ ^P(T([0-9]+H)?([0-9]+M)?([0-9]+S)?)?([0-9]+Y)?([0-9]+M)?([0-9]+D)?$ ]]; then
                return 0
            else
                return 1
            fi
            ;;
        *)
            echostd "Unknown argument type: $arg_type"
            return 1
            ;;
    esac
    return 0
}

# Get user input
get_user_input() {
    local field_info="$1"
    local default_value="$2"
    
    local field_name=$(echo "$field_info" | jq -r '.field')
    local description=$(echo "$field_info" | jq -r --arg field_name "$field_name" '.description // $field_name')
    # Check if referencedfield exists and has a type, otherwise use the main type
    local field_type=$(echo "$field_info" | jq -r '.referencedfield.type // .type')
    local mandatory=$(echo "$field_info" | jq -r '.mandatory // false')
    local modifier=$(echo "$field_info" | jq -r '.modifier // "user"')

    while true; do
        if [[ "$modifier" == "auto" ]]; then
            echo "$default_value"
            return
        fi
        
        # Add * to description if mandatory
        local prompt_description="$description"
        if [[ "$mandatory" == "true" ]]; then
            prompt_description="${description}*"
        fi
        
        read -p "Enter $prompt_description ($field_type): " user_input

        if [[ "$mandatory" == "true" && -z "$user_input" ]]; then
            echostd "$description is mandatory. Please provide a value."
            continue
        fi

        if [[ -z "$user_input" ]]; then
            echo "$default_value"
            return
        fi

        if is_valid "$user_input" "$field_type"; then
            echo "$user_input"
            return
        fi

        echostd "Invalid input. Please try again."
    done
}

# Create payload for the command
create_payload() {
    local args_info=$(echo "$1" | jq -c '.')
    local full_action="$2"
    local action="init"
    
    # Extract the second part of the action (e.g., "remove" from "event.remove")
    if [[ "$full_action" == *.* ]]; then
        action="${full_action##*.}"
    fi
    
    local payload="{}"

    if [[ "$args_info" == "{}" ]]; then
        payload=$(echo "$payload" | jq '. + {args: {}}')
    else
        payload=$(echo "$payload" | jq --argjson args_info "$args_info" '. + {args: $args_info}')
        payload=$(echo "$payload" | jq '. + {data: {}}')

        for field_key in $(echo "$args_info" | jq -r 'keys[]'); do
            local field_info=$(echo "$args_info" | jq -r --arg key "$field_key" '.[$key]')
            
            # Get field name (case-insensitive)
            local field_name=$(echo "$field_info" | jq -r '(keys_unsorted[] | select(ascii_downcase == "field")) as $key | .[$key] // ""')
            
            # Get description (case-insensitive)
            local description=$(echo "$field_info" | jq -r --arg field_name "$field_name" '(keys_unsorted[] | select(ascii_downcase == "description")) as $key | .[$key] // $field_name')
            
            # Get field type (case-insensitive)
            local field_type=$(echo "$field_info" | jq -r '(keys_unsorted[] | select(ascii_downcase == "type")) as $key | .[$key] // ""')
            
            # Get mandatory (case-insensitive)
            local mandatory=$(echo "$field_info" | jq -r '(keys_unsorted[] | select(ascii_downcase == "mandatory")) as $key | .[$key] // false')
            
            # Check if this action is supported for this field (case-insensitive)
            local supports=$(echo "$field_info" | jq -r '(keys_unsorted[] | select(ascii_downcase == "supports")) as $key | .[$key] // []')

            # Get modifier (case-insensitive)
            local modifier=$(echo "$field_info" | jq -r '(keys_unsorted[] | select(ascii_downcase == "modifier")) as $key | .[$key] // "user"')            

            if ! echo "$supports" | jq -e --arg action "$action" '.[] | select(. == $action)' > /dev/null 2>&1; then
                # Action not supported for this field, skip it
                continue
            fi

            # Skip fields with modifier "auto"
            if [[ "$modifier" == "auto" ]]; then
                continue
            fi

            # Get default value for this specific action (case-insensitive)
            local defaultValue=$(echo "$field_info" | jq -r --arg action "$action" '(keys_unsorted[] | select(ascii_downcase == "defaultvalue")) as $key | .[$key][$action] // ""')

            local user_input=$(get_user_input "$field_info" "$defaultValue")
            payload=$(echo "$payload" | jq --arg key "$field_key" --arg value "$user_input" '.data += {($key): $value}')
        done
    fi

    echo "$payload"
}

# Run the command with the provided identifier
run_command() {
    local selected_command="$1"
    local app_identifier="$2"

    local command_id=$(echo "$selected_command" | jq -r '.action')
    local args_info=$(echo "$selected_command" | jq -c '.args // {}')

    local payload="{\"identifier\": \"$app_identifier\", \"commands\": [{\"args\": {}, \"data\": {}, \"id\": \"$command_id\"}]}"

    if [[ "$args_info" != "null" ]]; then
        local args_payload=$(create_payload "$args_info" "$command_id")
        #payload=$(echo "$payload" | jq --argjson args "$args_payload" '.commands[0].args = $args.args')
        payload=$(echo "$payload" | jq --argjson args "$args_payload" '.commands[0].data = $args.data')
    fi

    echo "$payload"
    response=$(curl -s -X POST -H "Content-Type: application/json" -d "$payload" "$CMD_COMMANDS_URL")
    echo "Response from the server:"
    echo "$response" | jq .
}

# Display the app selection menu
display_app_menu() {
    local json="$1"
    local app_count=$(get_app_count "$json")

    clear_screen
    echo "Select an App"
    echo "=============="

    for (( i=0; i<app_count; i++ )); do
        local app_id=$(get_app_id_by_index "$json" "$i")
        local app_description=$(get_app_description_by_index "$json" "$i")
        echo "$((i + 1)): $app_id - $app_description"
    done

    echo "$((app_count + 1)): Exit"
}

# Handle app selection
handle_app_selection() {
    local json="$1"
    local app_count=$(get_app_count "$json")

    while true; do
        display_app_menu "$json"

        read -p "Select an app by number: " selection

        if [[ "$selection" -eq $((app_count + 1)) ]]; then
            echo "Exiting the program."
            exit 0
        elif [[ "$selection" -lt 1 || "$selection" -gt app_count ]]; then
            echo "Invalid selection, please try again."
            continue
        fi

        local app_index=$((selection - 1))
        local app_id=$(get_app_id_by_index "$json" "$app_index")
        local commands=($(get_app_commands_by_index "$json" "$app_index"))

        if [[ ${#commands[@]} -eq 0 ]]; then
            echo "No commands available for this app."
            read -p "Press Enter to continue..."
            continue
        fi

        handle_command_selection "$json" "$app_index" "$app_id" "${commands[@]}"
    done
}

# Display the command menu with pagination
display_command_menu() {
    local app_id="$1"
    shift
    local commands=("$@")
    local total=${#commands[@]}
    local start=$(( (page - 1) * COMMANDS_PER_PAGE ))
    local end=$(( start + COMMANDS_PER_PAGE ))

    clear_screen
    echo "Commands for: $app_id"
    echo "======================="

    for (( i=start; i<end && i<total; i++ )); do
        local command=$(decode_command "${commands[$i]}")
        local description=$(echo "$command" | jq -r '.description // "No description available."')
        local command_id=$(echo "$command" | jq -r '.id // "No ID"')
        echo "$((i + 1)): $description ($command_id)"
    done

    echo "$((total + 1)): Back"
    echo "$((total + 2)): Exit"
}

# Handle command selection
handle_command_selection() {
    local json="$1"
    local app_index="$2"
    local app_id="$3"
    shift 3
    local commands=("$@")
    local total=${#commands[@]}
    local page=1

    while true; do
        display_command_menu "$app_id" "${commands[@]}"

        read -p "Select a command by number: " selection

        if [[ "$selection" -eq $((total + 2)) ]]; then
            echo "Exiting the program."
            exit 0
        elif [[ "$selection" -eq $((total + 1)) ]]; then
            return
        elif [[ "$selection" -lt 1 || "$selection" -gt total ]]; then
            echo "Invalid selection, please try again."
            continue
        fi

        local command=$(decode_command "${commands[$((selection - 1))]}")
        if [[ -z "$command" ]]; then
            echo "Failed to decode command."
            continue
        fi

        # Check if the command has subcommands
        if echo "$command" | jq -e '.commands' &> /dev/null; then
            local command_id=$(echo "$command" | jq -r '.id // "subcommand"')
            handle_command_selection "$json" "$app_index" "$command_id" $(echo "$command" | jq -r '.commands[] | @base64')
        else
            run_command "$command" "$app_id"
            read -p "Press Enter to continue..."
        fi
    done
}

# Function to check service availability
check_service_availability() {
    if curl --output /dev/null --silent --head --fail "$GET_COMMANDS_URL"; then
        echo "Service is available."
        return 0
    else
        echo "Service is not available."
        return 1
    fi
}

# Main function
main() {
    local json=$(fetch_json)

    if [[ -z "$json" ]]; then
        echo "Failed to fetch commands."
        exit 1
    fi

    # Check if apps exist in JSON
    if ! echo "$json" | jq -e '.apps' &> /dev/null; then
        echo "No apps available in the JSON response."
        exit 1
    fi

    local app_count=$(get_app_count "$json")
    if [[ $app_count -eq 0 ]]; then
        echo "No apps available."
        exit 1
    fi

    handle_app_selection "$json"
}

# Start the script
if check_service_availability; then
    main
else
    echo "Exiting due to service unavailability."
    exit 1
fi
