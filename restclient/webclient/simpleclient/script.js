const PROXY_URL = 'proxy.php';
const COMMANDS_PER_PAGE = 5;

let currentLevel = 0;
let currentPage = 1;
let commandsStack = [];
let allCommands = [];

async function fetchCommands() {
    try {
        const response = await fetch(`${PROXY_URL}?action=request&endpoint=/get`, {
            method: 'GET'
        });
        const result = await response.json();
        
        if (result.success && result.data && result.data.apps) {
            return result.data.apps; // Return the apps with their commands
        } else {
            console.error('Error fetching commands:', result.error);
            return null;
        }
    } catch (error) {
        console.error('Error fetching commands:', error);
        return null;
    }
}

async function checkServiceStatus() {
    try {
        const response = await fetch(`${PROXY_URL}?action=status`);
        const result = await response.json();
        return result.status === 'online';
    } catch (error) {
        console.error('Error checking service status:', error);
        return false;
    }
}

function getPaginatedCommands(commands, page) {
    const startIndex = (page - 1) * COMMANDS_PER_PAGE;
    const endIndex = startIndex + COMMANDS_PER_PAGE;
    return commands.slice(startIndex, endIndex);
}

function getTotalPages(commands) {
    return Math.ceil(commands.length / COMMANDS_PER_PAGE);
}

function displayMenu(commands, level = 0, page = 1) {
    const paginatedCommands = getPaginatedCommands(commands, page);
    const totalPages = getTotalPages(commands);
    const startIndex = (page - 1) * COMMANDS_PER_PAGE;
    
    let menuHTML = `<div class="menu-container level-${level}">`;
    menuHTML += `<h2 class="menu-title">${'='.repeat(level + 1)} Menu ${'='.repeat(level + 1)}</h2>`;
    
    if (paginatedCommands.length === 0) {
        menuHTML += '<p class="no-commands">No more commands to display.</p>';
        menuHTML += '</div>';
        return menuHTML;
    }
    
    menuHTML += '<ul class="command-list">';
    
    paginatedCommands.forEach((command, idx) => {
        const description = command.description || 'No description available.';
        const commandId = command.id || 'No ID';
        const commandNumber = startIndex + idx + 1;
        menuHTML += `<li class="command-item" data-index="${idx}" data-command-number="${commandNumber}">
                        <span class="command-number">${commandNumber}</span>
                        <span class="command-description">${description}</span>
                        <span class="command-id">(${commandId})</span>
                    </li>`;
    });
    
    menuHTML += '</ul>';
    
    menuHTML += '<div class="menu-options">';
    
    if (totalPages > page) {
        menuHTML += `<button class="menu-button next-page-btn" data-action="next-page">
                        ${paginatedCommands.length + 1}: Next Page
                    </button>`;
    }
    
    if (level === 0) {
    } else {
        menuHTML += `<button class="menu-button back-btn" data-action="back">
                        Back to previous menu
                    </button>`;
    }
    
    menuHTML += '</div>';
    menuHTML += '</div>';
    
    return menuHTML;
}

function isValid(inputValue, argType) {
    let type = argType;
    
    if (type.includes('@')) {
        type = type.split('@').pop();
    }
    
    if (type === 'str') {
        return inputValue.trim() !== '';
    }
    
    if (type === 'int') {
        return /^-?\d+$/.test(inputValue);
    }
    
    if (type === 'unsigned') {
        return /^\d+$/.test(inputValue);
    }
    
    if (type === 'date') {
        const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
        if (!dateRegex.test(inputValue)) return false;
        const date = new Date(inputValue);
        return date instanceof Date && !isNaN(date);
    }
    
    if (type === 'time') {
        return /^\d{2}:\d{2}$/.test(inputValue);
    }
    
    if (type === 'duration') {
        return /^\d+h \d+m$/.test(inputValue);
    }
    
    return false;
}

function createFieldInputHTML(action, fieldKey, fieldInfo) {
    // Check if action is supported
    if (fieldInfo.supports && !fieldInfo.supports.includes(action)) {
        return '';
    }
    
    const fieldName = fieldInfo.field;
    const description = fieldInfo.description || fieldName;
    const fieldType = fieldInfo.type;
    console.log('📋 fieldInfo.type:', fieldInfo.type);
    console.log('📋 fieldInfo.referencedfield.type:', fieldInfo.referencedfield);
    const mandatory = fieldInfo.mandatory || false;
    const modifier = fieldInfo.modifier;
    
    // Extract default value for the specific action
    const defaultValue = getDefaultValueForAction(fieldInfo.defaultvalue, action);
    
    if (modifier === 'auto') {
        return `<input type="hidden" id="${fieldKey}" name="${fieldKey}" value="${defaultValue}" data-field-key="${fieldKey}" data-field-type="${fieldType}" data-mandatory="${mandatory}">`;
    }
    
    let inputHTML = `<div class="form-group">`;
    inputHTML += `<label for="${fieldKey}" class="form-group-label">
                    <span class="label-text">${description}</span>
                    <span class="field-type">(${fieldType})</span>`;
    
    if (mandatory) {
        inputHTML += `<span class="mandatory-indicator" aria-label="required">*</span>`;
    }
    
    inputHTML += `</label>`;
    inputHTML += `<input 
                    type="text" 
                    id="${fieldKey}" 
                    name="${fieldKey}" 
                    placeholder="Enter ${description}" 
                    data-field-key="${fieldKey}" 
                    data-field-type="${fieldType}" 
                    data-mandatory="${mandatory}" 
                    data-default-value="${defaultValue}">`;
    
    inputHTML += `<small class="field-hint"></small>`;
    inputHTML += `</div>`;
    
    return inputHTML;
}

/**
 * Extracts the default value for a specific action from the defaultvalue object.
 */
function getDefaultValueForAction(defaultvalueObj, action) {
    if (!defaultvalueObj || typeof defaultvalueObj !== 'object') {
        return '';
    }
    
    return defaultvalueObj[action] || '';
}

function displayCommandForm(selectedCommand, rootIdentifier, appId) {  // Add appId as a parameter
    console.log('🔍 displayCommandForm called');
    console.log('📋 Stack trace:', new Error().stack);
    console.log('📋 selectedCommand:', selectedCommand);
    console.log('📋 rootIdentifier:', rootIdentifier);
    
    let formHTML = `<div class="form-container">`;
    formHTML += `<div class="form-header">`;
    formHTML += `<h2>${selectedCommand.description}</h2>`;
    formHTML += `</div>`;

    if (selectedCommand.args && Object.keys(selectedCommand.args).length > 0) {
        console.log('✅ Form has arguments, creating form fields');
        formHTML += `<form id="command-form">`;
        
        for (const [fieldKey, fieldInfo] of Object.entries(selectedCommand.args)) {
            console.log(`  Creating field: ${fieldKey}`, fieldInfo);

            // Extract action: if it has two parts (e.g., "event.remove"), use the second part, otherwise use "init"
            const actionParts = selectedCommand.action.split('.');
            const action = actionParts.length > 1 ? actionParts[1] : 'init';

            // Check if referencedfield exists and has a type, otherwise use the main type
            if (fieldInfo.referencedfield?.type) {
                fieldInfo.type = fieldInfo.referencedfield.type;
            }

            const fieldHTML = createFieldInputHTML(action, fieldKey, fieldInfo);
            formHTML += fieldHTML;
        }
        
        formHTML += `<div class="form-actions">`;
        formHTML += `<button type="submit" class="btn-submit">Submit</button>`;
        formHTML += `<button type="button" class="btn-cancel" data-action="back">Cancel</button>`;
        formHTML += `</div>`;
        formHTML += `</form>`;
    } else {
        console.log('⚠️ No arguments for this command');
        formHTML += `<div class="no-args-message">`;
        formHTML += `<p>No arguments required for this command.</p>`;
        formHTML += `</div>`;
        formHTML += `<div class="form-actions">`;
        formHTML += `<button class="btn-submit" id="execute-no-args-btn">Execute</button>`;
        formHTML += `<button class="btn-cancel" data-action="back">Cancel</button>`;
        formHTML += `</div>`;
    }

    formHTML += `</div>`;
    
    const container = document.getElementById('menu-container');
    console.log('📍 Container found:', !!container);
    
    if (!container) {
        console.error('❌ Container not found!');
        return;
    }
    
    console.log('🧹 Clearing container and inserting form HTML');
    container.innerHTML = formHTML;
    
    // ✅ DEBUG: Check what was actually created
    console.log('🔍 Checking created form fields:');
    const allInputs = document.querySelectorAll('.field-input, input[type="hidden"]');
    console.log(`  Total inputs found: ${allInputs.length}`);
    allInputs.forEach((input, index) => {
        console.log(`  Input ${index}:`, {
            fieldKey: input.dataset.fieldKey,
            fieldType: input.dataset.fieldType,
            mandatory: input.dataset.mandatory,
            value: input.value,
            name: input.name
        });
    });
    
    const form = document.getElementById('command-form');
    console.log('📋 Form element found:', !!form);

    if (form) {
        console.log('✅ Attaching submit listener to form');
        form.addEventListener('submit', (e) => {
            console.log('🎯 Form submit event triggered!');
            e.preventDefault();
            console.log('🛑 preventDefault called, now calling runCommand');
            runCommand(selectedCommand, rootIdentifier, appId); // Pass appId here
        });
    } else {
        console.warn('⚠️ Form element NOT found');
    }
    
    const executeBtn = document.getElementById('execute-no-args-btn');
    if (executeBtn) {
        console.log('✅ Attaching click listener to execute button');
        executeBtn.addEventListener('click', () => {
            console.log('🎯 Execute button clicked!');
            runCommand(selectedCommand, rootIdentifier, appId); // Pass appId here
        });
    }
    
    const cancelBtn = document.querySelector('[data-action="back"]');
    if (cancelBtn) {
        console.log('✅ Attaching click listener to cancel button');
        cancelBtn.addEventListener('click', () => {
            console.log('🎯 Cancel button clicked!');
            currentLevel--;
            if (currentLevel < 0) currentLevel = 0;
            if (commandsStack.length > 0) {
                commandsStack.pop();
            }
            currentPage = 1;
            renderMenu();
        });
    }
}

async function runCommand(selectedCommand, rootIdentifier, appId) {  // Add appId as parameter
    const payload = {
        identifier: `${appId}`, // Updated to include appId
        commands: [{
            args: {},
            data: {},
            id: selectedCommand.action
        }]
    };
    
    if (selectedCommand.args && Object.keys(selectedCommand.args).length > 0) {
        const inputs = document.querySelectorAll('input');
        console.log('📝 Found inputs:', inputs.length);
        
        let validationPassed = true;
        
        // ✅ STEP 1: VALIDATE ALL FIELDS FIRST
        console.log('📋 Step 1: Validating all fields...');
        inputs.forEach(input => {
            const fieldKey = input.dataset.fieldKey;
            const fieldType = input.dataset.fieldType;
            const mandatory = input.dataset.mandatory === 'true';
            let value = input.value.trim();
            
            console.log(`✏️ Validating field: ${fieldKey} = "${value}" (type: ${fieldType}, mandatory: ${mandatory})`);
            
            const fieldInfo = selectedCommand.args[fieldKey];
            //payload.commands[0].args[fieldKey] = fieldInfo;
            
            // Check if mandatory field is empty
            if (value === '' && mandatory) {
                const hint = input.parentElement.querySelector('.field-hint');
                if (hint) {
                    hint.textContent = `${fieldInfo.description || fieldKey} is mandatory`;
                    hint.style.display = 'block';
                }
                validationPassed = false;
                console.warn(`  ❌ Mandatory field "${fieldKey}" is empty`);
                return;
            }
            
            // Skip optional empty fields (for now)
            if (value === '') {
                console.log(`  ⏭️ Field is optional and empty, will check for default later`);
                return;
            }
            
            // Validate format (only if field has a value)
            if (!isValid(value, fieldType)) {
                const hint = input.parentElement.querySelector('.field-hint');
                if (hint) {
                    hint.textContent = `Invalid ${fieldType} format`;
                    hint.style.display = 'block';
                }
                validationPassed = false;
                console.warn(`  ❌ Validation failed for ${fieldKey}: "${value}" is not a valid ${fieldType}`);
                return;
            } else {
                const hint = input.parentElement.querySelector('.field-hint');
                if (hint) {
                    hint.style.display = 'none';
                }
            }
        });
        
        if (!validationPassed) {
            console.error('🛑 Validation failed, stopping execution');
            return;
        }
        
        // ✅ STEP 2: ALL VALIDATION PASSED - NOW APPLY DEFAULTS AND BUILD PAYLOAD
        console.log('📋 Step 2: All validation passed. Applying defaults and building payload...');
        inputs.forEach(input => {
            const fieldKey = input.dataset.fieldKey;
            let value = input.value.trim();
            
            const fieldInfo = selectedCommand.args[fieldKey];
            
            // Extract action: if it has two parts (e.g., "event.remove"), use the second part, otherwise use "init"
            const actionParts = selectedCommand.action.split('.');
            const action = actionParts.length > 1 ? actionParts[1] : 'init';
            
            // Check if action is supported
            if (fieldInfo.supports && !fieldInfo.supports.includes(action)) {
                console.log(`  ⏭️ Field "${fieldKey}" does not support action "${action}", skipping`);
                return;
            }
            
            // Check if field is empty and has a default
            if (value === '') {
                console.log('📝 action:', fieldInfo.defaultvalue, action);
                const defaultValue = getDefaultValueForAction(fieldInfo.defaultvalue, action);
                if (defaultValue !== undefined && defaultValue !== null && defaultValue !== '') {
                    value = String(defaultValue).trim();
                    input.value = value;
                    console.log(`  ✅ Applied default for "${fieldKey}": "${value}"`);
                } else {
                    // Optional field with no default - skip it
                    console.log(`  ⏭️ Field "${fieldKey}" is empty with no default, skipping`);
                    return;
                }
            }
            
            // Add to payload as STRING (no type conversion)
            payload.commands[0].data[fieldKey] = value;
            console.log(`  ✅ Added to payload as string: "${value}"`);
        });
    }
    
    console.log('📦 Final payload:', JSON.stringify(payload, null, 2));
    console.log('🌐 Sending fetch request to:', `${PROXY_URL}?action=request&endpoint=/cmd`);
    
    // ✅ STEP 3: SEND REQUEST
    try {
        const response = await fetch(`${PROXY_URL}?action=request&endpoint=/cmd`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });
        
        console.log('📥 Response received. Status:', response.status);
        console.log('📥 Response OK?:', response.ok);
        
        const result = await response.json();
        console.log('✅ JSON parsed:', JSON.stringify(result, null, 2));
        console.log('📊 Result has success property?:', 'success' in result);
        console.log('📊 Result.success value:', result.success);
        
        displayCommandResponse(result, payload);
        console.log('✅ displayCommandResponse called');
    } catch (error) {
        console.error('❌ Error sending command:', error);
        console.error('❌ Error stack:', error.stack);
        displayCommandResponse({
            success: false,
            error: error.message
        }, payload);
    }
}

function displayCommandResponse(response, payload) {
    console.log('🎨 displayCommandResponse called with:', { response, payload });
    
    const container = document.getElementById('menu-container');
    console.log('📍 Container found:', !!container);
    
    if (!container) {
        console.error('❌ Container not found! Cannot display response');
        return;
    }
    
    container.innerHTML = '';
    console.log('🧹 Container cleared');
    
    let responseHTML = `<div class="response-container">`;
    responseHTML += `<h3 class="response-title">Command Execution Details</h3>`;
    
    // ✅ SHOW PAYLOAD SENT
    responseHTML += `<div class="response-section">`;
    responseHTML += `<h4 class="section-title">📤 Payload Sent:</h4>`;
    if (payload) {
        const payloadJSON = JSON.stringify(payload, null, 2);
        const highlightedPayload = syntaxHighlight(payloadJSON);
        responseHTML += `<div class="response-data payload-data">${highlightedPayload}</div>`;
    } else {
        responseHTML += `<div class="response-data"><p>No payload (no arguments command)</p></div>`;
    }
    responseHTML += `</div>`;
    
    // ✅ SHOW RESPONSE RECEIVED
    responseHTML += `<div class="response-section">`;
    responseHTML += `<h4 class="section-title">📥 Response Received:</h4>`;
    
    if (response.success) {
        console.log('✅ Response is successful');
        responseHTML += `<div class="response-status success">✅ Success</div>`;
        const jsonString = JSON.stringify(response.data || response, null, 2);
        const highlightedJSON = syntaxHighlight(jsonString);
        responseHTML += `<div class="response-data">${highlightedJSON}</div>`;
    } else {
        console.warn('⚠️ Response indicates error');
        responseHTML += `<div class="response-status error">❌ Error</div>`;
        responseHTML += `<div class="response-error">`;
        responseHTML += `<p><strong>Error:</strong> ${response.error || 'Unknown error'}</p>`;
        if (response.details) {
            responseHTML += `<p><strong>Details:</strong> ${response.details}</p>`;
        }
        responseHTML += `</div>`;
    }
    responseHTML += `</div>`;
    
    responseHTML += `<button class="continue-btn" data-action="back">Continue</button>`;
    responseHTML += `</div>`;
    
    console.log('📄 Response HTML built, length:', responseHTML.length);
    container.innerHTML = responseHTML;
    console.log('📄 Response HTML inserted into DOM');
    
    const continueBtn = document.querySelector('.continue-btn');
    console.log('🔘 Continue button found:', !!continueBtn);
    
    if (continueBtn) {
        console.log('✅ Attaching click listener to continue button');
        continueBtn.addEventListener('click', () => {
            console.log('🎯 Continue button clicked!');
            currentLevel--;
            if (currentLevel < 0) currentLevel = 0;
            if (commandsStack.length > 0) {
                commandsStack.pop();
            }
            currentPage = 1;
            renderMenu();
        });
    } else {
        console.error('❌ Continue button not found!');
    }
}


function renderMenu() {
    let currentCommands = commandsStack[currentLevel - 1] || []; // Adjust as per the stack

    if (currentLevel > 1) {
        currentCommands = commandsStack[currentLevel - 1].commands || []; // Access subcommands
    }

    const menuHTML = displayMenu(currentCommands, currentLevel, currentPage);
    const container = document.getElementById('menu-container');
    container.innerHTML = menuHTML;

    setupCommandItemListeners(currentCommands); // Set up event listeners for the current level commands

    const backBtn = document.querySelector('.back-btn');
    if (backBtn) {
        backBtn.addEventListener('click', () => {
            currentLevel--;
            if (currentLevel < 0) currentLevel = 0;
            if (commandsStack.length > 0) {
                commandsStack.pop();
            }
            if (currentLevel > 0 ){
                renderMenu();
            }
            else{
                renderAppMenu();
            }
        });
    }
}

function setupCommandItemListeners(currentCommands) {
    const commandItems = document.querySelectorAll('.command-item');
    commandItems.forEach(item => {
        item.addEventListener('click', () => {
            const index = parseInt(item.dataset.index);
            const paginatedCommands = getPaginatedCommands(currentCommands, currentPage);
            const selectedCommand = paginatedCommands[index];
            const appId = commandsStack[currentLevel - 1].id; // Get appId from the command's stack

            if (selectedCommand.commands) {
                // If the selected command has its own commands (sub-menu)
                commandsStack.push(selectedCommand); // Push command onto the stack
                currentLevel++;
                currentPage = 1;
                renderMenu(); // Render next level
            } else {
                // Ensure we're fetching the correct command from the app structure
                const commandFromApp = commandsStack[currentLevel - 1].commands.find(cmd => cmd.id === selectedCommand.id);

                // If it's a terminal command, show the command form
                const rootIdentifier = currentCommands.id || 'root';
                displayCommandForm(commandFromApp, rootIdentifier, appId); // Pass the correct command
            }
        });
    });
}


function renderAppMenu() {
    const menuHTML = displayMenu(allApps, currentLevel, currentPage); // Using apps instead of commands
    const container = document.getElementById('menu-container');
    container.innerHTML = menuHTML;

    const appItems = document.querySelectorAll('.command-item');
    appItems.forEach(item => {
        item.addEventListener('click', () => {
            const index = parseInt(item.dataset.index);
            const selectedApp = allApps[index];
            commandsStack.push(selectedApp.commands); // Push commands of the selected app
            currentLevel++;
            currentPage = 1;
            renderMenu(); // Render commands for the selected app
        });
    });
}

async function initializeApp() {
    const container = document.getElementById('menu-container');
    
    container.innerHTML = '<div class="loading"><p>Checking service availability...</p></div>';
    
    const isAvailable = await checkServiceStatus();
    
    if (!isAvailable) {
        container.innerHTML = '<div class="error"><p>Service is not available. Please try again later.</p></div>';
        return;
    }
    
    container.innerHTML = '<div class="loading"><p>Loading applications...</p></div>';
    
    allApps = await fetchCommands();  // Fetch apps instead of commands
    
    if (allApps === null) {
        container.innerHTML = '<div class="error"><p>Error loading applications. Please try again later.</p></div>';
        return;
    }
    
    currentLevel = 0;
    currentPage = 1;
    commandsStack = [];
    
    renderAppMenu(); // Render app menu instead of command menu
}

/**
 * Initialize the application on page load
 */
window.addEventListener('load', function() {
    loadTheme();
    initializeApp();
    setupEventListeners();
});

/**
 * Setup event listeners for interactive elements
 */
function setupEventListeners() {
    const endpointInput = document.getElementById('endpoint');
    
    if (endpointInput) {
        endpointInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                initializeApp();
            }
        });
    }
}

/**
 * Toggle between light and dark mode
 */
function toggleTheme() {
    const html = document.documentElement;
    const body = document.body;
    const themeSwitch = document.getElementById('themeSwitch');
    const themeLabel = document.getElementById('themeLabel');
    
    body.classList.toggle('light-mode');
    themeSwitch.classList.toggle('active');
    
    if (body.classList.contains('light-mode')) {
        themeLabel.textContent = 'Light';
        html.setAttribute('data-theme', 'light');
        localStorage.setItem('theme', 'light');
    } else {
        themeLabel.textContent = 'Dark';
        html.setAttribute('data-theme', 'dark');
        localStorage.setItem('theme', 'dark');
    }
}


/**
 * Load theme preference from localStorage
 */
function loadTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    const html = document.documentElement;
    const body = document.body;
    const themeSwitch = document.getElementById('themeSwitch');
    const themeLabel = document.getElementById('themeLabel');
    
    html.setAttribute('data-theme', savedTheme);
    
    if (savedTheme === 'light') {
        body.classList.add('light-mode');
        themeSwitch.classList.add('active');
        themeLabel.textContent = 'Light';
    } else {
        body.classList.remove('light-mode');
        themeSwitch.classList.remove('active');
        themeLabel.textContent = 'Dark';
    }
}


/**
 * Syntax highlight JSON with proper formatting
 */
function syntaxHighlight(json) {
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
        let cls = 'json-number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'json-key';
            } else {
                cls = 'json-string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'json-boolean';
        } else if (/null/.test(match)) {
            cls = 'json-null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
}
