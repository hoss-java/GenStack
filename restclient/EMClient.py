import requests
from requests.exceptions import RequestException
import json
import os
import sys
import platform
import re
from datetime import datetime

# Constants
BASE_URL = "http://172.32.0.11:32768/api"
GET_COMMANDS_URL = f"{BASE_URL}/get"
CMD_COMMANDS_URL = f"{BASE_URL}/cmd"
COMMANDS_PER_PAGE = 5

def clear_screen():
    """Clears the terminal screen for a fresh menu display."""
    os.system('cls' if platform.system() == 'Windows' else 'clear')

def fetch_commands():
    """Fetches the list of commands from the API.

    Returns:
        dict: The full response with apps and commands.
    """
    try:
        response = requests.get(GET_COMMANDS_URL)
        response.raise_for_status()
        return response.json()
    except requests.RequestException as e:
        print(f"Error fetching commands: {e}")
        return None

def fetch_apps(data):
    """Extracts the list of apps from the response.

    Args:
        data (dict): The full API response.

    Returns:
        list: A list of app dictionaries.
    """
    return data.get('apps', [])

def select_app(apps):
    """Displays available apps and prompts user to select one.

    Args:
        apps (list): List of app dictionaries.

    Returns:
        dict: The selected app object, or None if user cancels.
    """
    clear_screen()
    print("\n" + "=" * 20 + " Available Apps " + "=" * 20)
    
    if not apps:
        print("No apps available.")
        return None

    for idx, app in enumerate(apps, 1):
        description = app.get('description', 'No description available.')
        app_id = app.get('id', 'No ID')
        print(f"{idx}: {description} ({app_id})")

    print(f"{len(apps) + 1}: Exit")

    while True:
        try:
            selection = int(input("\nSelect an app by number: "))
            
            if selection == len(apps) + 1:
                print("Exiting the program.")
                exit(0)
            
            if 1 <= selection <= len(apps):
                return apps[selection - 1]
            else:
                print("Invalid selection, please try again.")
        except ValueError:
            print("Invalid input, please enter a number.")

def display_menu(commands, level=0, page=1):
    """Displays the command menu with pagination.

    Args:
        commands (list): List of command dictionaries.
        level (int): The current menu level.
        page (int): The current page number.

    Returns:
        bool: True if commands were displayed successfully, else False.
    """
    start_index = (page - 1) * COMMANDS_PER_PAGE
    end_index = start_index + COMMANDS_PER_PAGE
    paginated_commands = commands[start_index:end_index]

    clear_screen()
    print("\n" + ("=" * (level + 1)) + " Menu " + ("=" * (level + 1)))

    if not paginated_commands:
        print("No more commands to display.")
        return False

    for idx, command in enumerate(paginated_commands):
        description = command.get('description', 'No description available.')
        command_id = command.get('id', 'No ID')
        print(f"{start_index + idx + 1}: {description} ({command_id})")

    if level == 0:
        print(f"{len(paginated_commands) + 1}: Exit")
    else:
        print(f"{len(paginated_commands) + 1}: Back to previous menu")

    return True

def is_valid(input_value, arg_type):
    """Validates user input based on field type.

    Args:
        input_value (str): The value to validate.
        arg_type (str): The type of the argument.

    Returns:
        bool: True if input is valid, else False.
    """
    if '@' in arg_type:
        arg_type = arg_type.split('@')[-1]

    if arg_type == "str":
        if input_value.strip() == "":
            print("Hint: Input should be a non-empty string.")
            return False
        return True

    if arg_type == "int" and re.match(r"-?\d+", input_value):
        return True
    elif arg_type == "unsigned" and re.match(r"\d+", input_value):
        return True
    elif arg_type == "date":
        try:
            datetime.strptime(input_value, "%Y-%m-%d")
            return True
        except ValueError:
            print("Hint: Input should be a valid date in the format YYYY-MM-DD.")
            return False
    elif arg_type == "time":
        try:
            datetime.strptime(input_value, "%H:%M")
            return True
        except ValueError:
            print("Hint: Input should be a valid time in HH:mm format.")
            return False
    elif arg_type == "duration" and re.match(r"^\d+h \d+m$", input_value):
        return True

    print(f"Hint: Unknown argument type: {arg_type}")
    return False

def get_action_type(selected_command):
    """Extracts the action type from the command.
    
    Args:
        selected_command (dict): Dictionary of the selected command.
    
    Returns:
        str: The action type (e.g., 'add' from 'event.add', or 'init' if no dot exists).
    """
    action = selected_command['action']
    if '.' in action:
        return action.split('.')[1]
    return 'init'


def get_default_value(field_info, action):
    """Gets the default value for a field based on the action.
    
    Args:
        field_info (dict): Field information including defaultValue.
        action (str): The current action type.
    
    Returns:
        str: The default value for the action, or empty string if not found.
    """
    default_value = field_info.get('defaultvalue', {})
    
    # If defaultValue is a string (old format), return it
    if isinstance(default_value, str):
        return default_value
    
    # If defaultValue is a dict (new format), get value for the action
    if isinstance(default_value, dict):
        return default_value.get(action, '')
    
    return ''


def is_field_supported(field_info, action):
    """Checks if the field supports the current action.
    
    Args:
        field_info (dict): Field information including supports list.
        action (str): The current action type.
    
    Returns:
        bool: True if the field supports the action or has no supports restriction.
    """
    supports = field_info.get('supports', [])
    
    # If no supports list, field is supported for all actions
    if not supports:
        return True
    
    # Check if current action is in the supports list
    return action in supports


def get_user_input(field_info, action):
    """Prompts user for input based on field information and action.

    Args:
        field_info (dict): Field information including name, type, etc.
        action (str): The current action type.

    Returns:
        str: User input value or default value.
    """
    field_name = field_info['field']
    description = field_info.get('description', field_name)
    # Check if referencedfield exists and has a type, otherwise use the main type
    referenced_field = field_info.get('referencedfield')
    field_type = referenced_field.get('type') if referenced_field else field_info.get('type')

    mandatory = field_info.get('mandatory', False)
    modifier = field_info.get('modifier')
    default_value = get_default_value(field_info, action)

    if modifier == "auto":
        return default_value

    # Add * to description if mandatory
    prompt_description = f"{description}*" if mandatory else description

    while True:
        user_input = input(f"Enter {prompt_description} ({field_type}): ")

        if mandatory and user_input.strip() == "":
            print(f"{description} is mandatory. Please provide a value.")
            continue

        if not mandatory and user_input.strip() == "":
            return default_value

        if is_valid(user_input, field_type):
            return int(user_input) if field_type == "int" else user_input

        print("Invalid input. Please try again.")

def create_payload(args_info, action):
    """Creates a JSON payload from user inputs.

    Args:
        args_info (dict): Dictionary of field information.
        action (str): The current action type.

    Returns:
        dict: Dictionary representing the payload.
    """
    payload = {}

    for field_key, field_info in args_info.items():
        # Check if field supports current action
        if not is_field_supported(field_info, action):
            continue
        
        user_input = get_user_input(field_info, action)
        payload[field_key] = user_input

    return payload

def run_command(selected_command, root_identifier, app_id):
    """Executes the command with the provided identifier.

    Args:
        selected_command (dict): Dictionary of the selected command.
        root_identifier (str): Identifier from the root.
        app_id (str): The ID of the selected app.
    """
    action = get_action_type(selected_command)
    
    payload = {
        "identifier": f"{root_identifier}",
        "commands": [{
            "args": {},
            "data": {},
            "id": selected_command['action']
        }]
    }

    if 'args' in selected_command:
        for field_key, field_info in selected_command['args'].items():
            # Check if field supports current action
            if not is_field_supported(field_info, action):
                continue
            
            user_input = get_user_input(field_info, action)
            if user_input is not None:
                payload['commands'][0]['data'][field_key] = user_input

    try:
        print("Payload Sent:")
        print(json.dumps(payload, indent=2))
        response = requests.post(CMD_COMMANDS_URL, json=payload)
        response.raise_for_status()
        print("Response from the server:")
        print(json.dumps(response.json(), indent=2))
    except requests.RequestException as e:
        print(f"Error sending command: {e}")

def handle_selection(commands, app_id, level=0):
    """Handles user selection from the menu.

    Args:
        commands (list): List of command dictionaries.
        app_id (str): The ID of the selected app.
        level (int): The current menu level.
    """
    page = 1

    while True:
        displayed = display_menu(commands, level, page)

        if not displayed:
            break

        selection = input("Select a command by number: ")

        try:
            selection = int(selection)
        except ValueError:
            print("Invalid selection, please enter a number.")
            continue

        if level == 0 and selection == len(commands) + 1:
            print("Exiting the program.")
            exit(0)
        elif selection == len(commands) + 1:
            page += 1
            continue
        elif selection == len(commands) + 2:
            return

        if 1 <= selection <= len(commands):
            selected_command = commands[selection - 1]
            if 'commands' in selected_command:
                handle_selection(selected_command['commands'], app_id, level + 1)
            else:
                root_identifier = selected_command.get('id', 'root').split('.')[0] + '@' + app_id
                run_command(selected_command, root_identifier, app_id)
                input("Press Enter to continue...")
        else:
            print("Invalid selection, please try again.")

def main():
    """Main function to execute the command-line interface."""
    data = fetch_commands()
    if data is None:
        return

    apps = fetch_apps(data)
    if not apps:
        print("No apps available.")
        return

    selected_app = select_app(apps)
    if selected_app is None:
        return

    app_id = selected_app.get('id')
    app_commands = selected_app.get('commands', [])
    if not app_commands:
        print(f"No commands available for app: {app_id}")
        return

    clear_screen()
    print(f"\nLoaded app: {selected_app.get('description', app_id)}")
    print("Starting command menu...\n")
    input("Press Enter to continue...")

    while True:
        clear_screen()
        display_menu(app_commands)
        handle_selection(app_commands, app_id)

def check_service_availability():
    try:
        response = requests.get(GET_COMMANDS_URL, timeout=5)
        response.raise_for_status()
        print("Service is available.")
        return True
    except RequestException as e:
        print(f"Service is not available")
        return False

if __name__ == "__main__":
    if check_service_availability():
        main()
    else:
        print("Exiting due to service unavailability.")
        exit(1)
