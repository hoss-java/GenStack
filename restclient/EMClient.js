// Constants
const BASE_URL = "http://172.32.0.11:32768/api";
const GET_COMMANDS_URL = `${BASE_URL}/get`;
const CMD_COMMANDS_URL = `${BASE_URL}/cmd`;
const COMMANDS_PER_PAGE = 5;

const readline = require("readline");

// Create a single readline interface that persists throughout the application
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
});

// Utility function to prompt user
function prompt(question) {
  return new Promise((resolve) => {
    rl.question(question, (answer) => {
      resolve(answer);
    });
  });
}

// Utility Functions
function clearScreen() {
  /**
   * Clears the console
   */
  console.clear();
}

async function fetchJson() {
  /**
   * Fetches the entire JSON from the API.
   * @returns {Promise<Object|null>} The JSON object or null on error
   */
  try {
    const response = await fetch(GET_COMMANDS_URL);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const data = await response.json();
    return data;
  } catch (error) {
    console.error(`Error fetching JSON: ${error.message}`);
    return null;
  }
}

function displayAppMenu(apps) {
  /**
   * Displays the app selection menu.
   * @param {Array} apps - List of app objects
   */
  clearScreen();
  console.log("\n" + "=".repeat(5) + " Select an App " + "=".repeat(5));

  apps.forEach((app, idx) => {
    const appId = app.id || "Unknown";
    const description = app.description || "No description available.";
    console.log(`${idx + 1}: ${appId} - ${description}`);
  });

  console.log(`${apps.length + 1}: Exit`);
}

function displayCommandMenu(appId, commands, page = 1) {
  /**
   * Displays the command menu with pagination.
   * @param {string} appId - The selected app ID
   * @param {Array} commands - List of command objects
   * @param {number} page - Current page number
   * @returns {Object} Object containing display info and paginated commands
   */
  const startIndex = (page - 1) * COMMANDS_PER_PAGE;
  const endIndex = startIndex + COMMANDS_PER_PAGE;
  const paginatedCommands = commands.slice(startIndex, endIndex);

  clearScreen();
  console.log("\n" + "=".repeat(5) + ` Commands for: ${appId} ` + "=".repeat(5));

  if (paginatedCommands.length === 0) {
    console.log("No more commands to display.");
    return { 
      displayed: false, 
      paginatedCommands: [], 
      totalCommands: commands.length 
    };
  }

  paginatedCommands.forEach((command, idx) => {
    const description = command.description || "No description available.";
    const commandId = command.id || "No ID";
    console.log(`${startIndex + idx + 1}: ${description} (${commandId})`);
  });

  const nextOptionNumber = paginatedCommands.length + 1;
  console.log(`${nextOptionNumber}: Back`);
  console.log(`${nextOptionNumber + 1}: Exit`);

  return {
    displayed: true,
    paginatedCommands,
    totalCommands: commands.length,
    currentPage: page,
    backOption: nextOptionNumber,
    exitOption: nextOptionNumber + 1,
  };
}

function isValid(inputValue, argType) {
  /**
   * Validates user input based on field type.
   * @param {string} inputValue - The value to validate
   * @param {string} argType - The type of the argument
   * @returns {boolean} True if input is valid
   */
  let type = argType;
  if (argType.includes("@")) {
    type = argType.split("@").pop();
  }

  if (type === "str") {
    if (inputValue.trim() === "") {
      console.log("Hint: Input should be a non-empty string.");
      return false;
    }
    return true;
  }

  if (type === "int" && /^-?\d+$/.test(inputValue)) {
    return true;
  } else if (type === "unsigned" && /^\d+$/.test(inputValue)) {
    return true;
  } else if (type === "date") {
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (!dateRegex.test(inputValue)) {
      console.log("Hint: Input should be a valid date in the format YYYY-MM-DD.");
      return false;
    }
    const date = new Date(inputValue);
    return !isNaN(date.getTime());
  } else if (type === "time") {
    const timeRegex = /^([0-1][0-9]|2[0-3]):[0-5][0-9]$/;
    if (!timeRegex.test(inputValue)) {
      console.log("Hint: Input should be a valid time in HH:mm format.");
      return false;
    }
    return true;
  } else if (type === "duration" && /^\d+h \d+m$/.test(inputValue)) {
    return true;
  }

  console.log(`Hint: Unknown argument type: ${type}`);
  return false;
}

function getActionType(selectedCommand) {
  /**
   * Extracts the action type from the command.
   * @param {Object} selectedCommand - The selected command object
   * @returns {string} The action type (e.g., 'add' from 'event.add', or 'init' if no dot exists)
   */
  const action = selectedCommand.action;
  if (action.includes(".")) {
    return action.split(".")[1];
  }
  return "init";
}

function getDefaultValue(fieldInfo, action) {
  /**
   * Gets the default value for a field based on the action.
   * @param {Object} fieldInfo - Field information including defaultValue
   * @param {string} action - The current action type
   * @returns {string} The default value for the action, or empty string if not found
   */
  const defaultValue = fieldInfo.defaultvalue || {};

  // If defaultValue is a string (old format), return it
  if (typeof defaultValue === "string") {
    return defaultValue;
  }

  // If defaultValue is an object (new format), get value for the action
  if (typeof defaultValue === "object") {
    return defaultValue[action] || "";
  }

  return "";
}

function isFieldSupported(fieldInfo, action) {
  /**
   * Checks if the field supports the current action.
   * @param {Object} fieldInfo - Field information including supports list
   * @param {string} action - The current action type
   * @returns {boolean} True if the field supports the action or has no supports restriction
   */
  const supports = fieldInfo.supports || [];

  // If no supports list, field is supported for all actions
  if (supports.length === 0) {
    return true;
  }

  // Check if current action is in the supports list
  return supports.includes(action);
}

async function getUserInput(fieldInfo, action) {
  /**
   * Prompts user for input based on field information and action.
   * @param {Object} fieldInfo - Field information including name, type, etc.
   * @param {string} action - The current action type
   * @returns {Promise<string|number>} User input value or default value
   */
  const fieldName = fieldInfo.field;
  const description = fieldInfo.description || fieldName;
  // Check if referencedfield exists and has a type, otherwise use the main type
  const fieldType = fieldInfo.referencedfield?.type || fieldInfo.type;
  const mandatory = fieldInfo.mandatory || false;
  const modifier = fieldInfo.modifier;
  const defaultValue = getDefaultValue(fieldInfo, action);

  if (modifier === "auto") {
    return defaultValue;
  }

  // Add * to description if mandatory
  const promptDescription = mandatory ? `${description}*` : description;

  while (true) {
    const userInput = await prompt(`Enter ${promptDescription} (${fieldType}): `);

    if (mandatory && userInput.trim() === "") {
      console.log(`${description} is mandatory. Please provide a value.`);
      continue;
    }

    if (!mandatory && userInput.trim() === "") {
      return defaultValue;
    }

    if (isValid(userInput, fieldType)) {
      return fieldType === "int" ? parseInt(userInput) : userInput;
    }

    console.log("Invalid input. Please try again.");
  }
}

async function createPayload(argsInfo, action) {
  /**
   * Creates a JSON payload from user inputs.
   * @param {Object} argsInfo - Dictionary of field information
   * @param {string} action - The current action type
   * @returns {Promise<Object>} Dictionary representing the payload
   */
  const payload = {};

  for (const [fieldKey, fieldInfo] of Object.entries(argsInfo)) {
    // Check if field supports current action
    if (!isFieldSupported(fieldInfo, action)) {
      continue;
    }

    // Skip fields with modifier "auto"
    if (fieldInfo.modifier === "auto") {
      continue;
    }

    const userInput = await getUserInput(fieldInfo, action);
    payload[fieldKey] = userInput;
  }

  return payload;
}

async function runCommand(selectedCommand, appId) {
  /**
   * Executes the command with the provided app identifier.
   * @param {Object} selectedCommand - The selected command object
   * @param {string} appId - The app ID
   */
  const action = getActionType(selectedCommand);

  const payload = {
    identifier: appId,
    commands: [
      {
        args: {},
        data: {},
        id: selectedCommand.action,
      },
    ],
  };

  if (selectedCommand.args) {
    for (const [fieldKey, fieldInfo] of Object.entries(selectedCommand.args)) {
      // Check if field supports current action
      if (!isFieldSupported(fieldInfo, action)) {
        continue;
      }

      // Skip fields with modifier "auto"
      if (fieldInfo.modifier === "auto") {
        continue;
      }

      const userInput = await getUserInput(fieldInfo, action);
      //payload.commands[0].args[fieldKey] = fieldInfo;
      if (userInput !== null) {
        payload.commands[0].data[fieldKey] = userInput;
      }
    }
  }

  try {
    const response = await fetch(CMD_COMMANDS_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    console.log("Payload Sent:");
    console.log(JSON.stringify(payload, null, 2));
    const responseData = await response.json();
    console.log("Response from the server:");
    console.log(JSON.stringify(responseData, null, 2));
  } catch (error) {
    console.error(`Error sending command: ${error.message}`);
  }
}

async function handleAppSelection(json) {
  /**
   * Handles app selection from the menu.
   * @param {Object} json - The full JSON object containing apps
   */
  const apps = json.apps || [];

  if (apps.length === 0) {
    console.log("No apps available.");
    return;
  }

  while (true) {
    displayAppMenu(apps);

    const selection = await prompt("Select an app by number: ");
    let selectedNum;

    try {
      selectedNum = parseInt(selection);
    } catch (error) {
      console.log("Invalid selection, please enter a number.");
      continue;
    }

    // Handle "Exit" option
    if (selectedNum === apps.length + 1) {
      console.log("Exiting the program.");
      rl.close();
      process.exit(0);
    }

    // Handle app selection
    if (selectedNum >= 1 && selectedNum <= apps.length) {
      const selectedApp = apps[selectedNum - 1];
      const appId = selectedApp.id;
      const commands = selectedApp.commands || [];

      if (commands.length === 0) {
        console.log("No commands available for this app.");
        await prompt("Press Enter to continue...");
        continue;
      }

      await handleCommandSelection(appId, commands);
    } else {
      console.log("Invalid selection, please try again.");
    }
  }
}

async function handleCommandSelection(appId, commands, level = 0) {
  /**
   * Handles command selection from the menu.
   * @param {string} appId - The selected app ID
   * @param {Array} commands - List of command objects
   * @param {number} level - Current menu level (for nested commands)
   */
  let page = 1;

  while (true) {
    const menuInfo = displayCommandMenu(appId, commands, page);

    if (!menuInfo.displayed) {
      break;
    }

    const selection = await prompt("Select a command by number: ");
    let selectedNum;

    try {
      selectedNum = parseInt(selection);
    } catch (error) {
      console.log("Invalid selection, please enter a number.");
      continue;
    }

    const paginatedCommands = menuInfo.paginatedCommands;
    const backOption = menuInfo.backOption;
    const exitOption = menuInfo.exitOption;

    // Handle "Back" option
    if (selectedNum === backOption) {
      return;
    }

    // Handle "Exit" option
    if (selectedNum === exitOption) {
      console.log("Exiting the program.");
      rl.close();
      process.exit(0);
    }

    // Handle command selection
    if (selectedNum >= 1 && selectedNum <= paginatedCommands.length) {
      const selectedCommand = paginatedCommands[selectedNum - 1];

      if (selectedCommand.commands && selectedCommand.commands.length > 0) {
        // Nested menu
        await handleCommandSelection(selectedCommand.id || appId, selectedCommand.commands, level + 1);
      } else {
        // Execute command
        await runCommand(selectedCommand, appId);
        await prompt("Press Enter to continue...");
      }
    } else {
      console.log("Invalid selection, please try again.");
    }
  }
}

async function main() {
  /**
   * Main function to execute the command-line interface.
   */
  const json = await fetchJson();
  if (json === null) {
    return;
  }

  // Check if apps exist in JSON
  if (!json.apps || json.apps.length === 0) {
    console.log("No apps available in the JSON response.");
    rl.close();
    process.exit(1);
  }

  await handleAppSelection(json);
}

async function checkServiceAvailability() {
  /**
   * Checks if the REST service is available.
   * @returns {Promise<boolean>} True if service is available
   */
  try {
    const response = await fetch(GET_COMMANDS_URL, { timeout: 5000 });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    console.log("Service is available.");
    return true;
  } catch (error) {
    console.log("Service is not available");
    return false;
  }
}

// Main execution
(async () => {
  if (await checkServiceAvailability()) {
    await main();
  } else {
    console.log("Exiting due to service unavailability.");
    rl.close();
    process.exit(1);
  }
})();
