/**
 * REST API Tester - Main Application
 * Handles API testing, theme switching, and UI interactions
 */

const PROXY_URL = 'proxy.php';

/**
 * Initialize the application on page load
 */
window.addEventListener('load', function() {
    loadTheme();
    checkStatus();
    setupEventListeners();
});

/**
 * Setup event listeners for interactive elements
 */
function setupEventListeners() {
    const endpointInput = document.getElementById('endpoint');
    
    endpointInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            testAPI();
        }
    });
}

/**
 * Toggle between light and dark mode
 */
function toggleTheme() {
    const body = document.body;
    const themeSwitch = document.getElementById('themeSwitch');
    const themeLabel = document.getElementById('themeLabel');
    
    body.classList.toggle('light-mode');
    themeSwitch.classList.toggle('active');
    
    if (body.classList.contains('light-mode')) {
        themeLabel.textContent = 'Light';
        localStorage.setItem('theme', 'light');
    } else {
        themeLabel.textContent = 'Dark';
        localStorage.setItem('theme', 'dark');
    }
}

/**
 * Load theme preference from localStorage
 */
function loadTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    const body = document.body;
    const themeSwitch = document.getElementById('themeSwitch');
    const themeLabel = document.getElementById('themeLabel');
    
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
 * Check REST service status
 */
async function checkStatus() {
    const indicator = document.getElementById('statusIndicator');
    const statusText = document.getElementById('statusText');
    const statusDetails = document.getElementById('statusDetails');
    
    indicator.className = 'status-indicator checking';
    statusText.textContent = 'Checking...';
    statusDetails.textContent = 'Connecting to REST service...';
    
    try {
        const response = await fetch(`${PROXY_URL}?action=status`);
        const data = await response.json();
        
        if (data.status === 'online') {
            indicator.className = 'status-indicator online';
            statusText.textContent = '✅ Online';
            statusDetails.textContent = `${data.url} (HTTP ${data.httpCode})`;
            showAlert('Service is online and ready!', 'success');
            document.getElementById('sendBtn').disabled = false;
        } else {
            indicator.className = 'status-indicator offline';
            statusText.textContent = '❌ Offline';
            statusDetails.textContent = data.error || 'Service is not responding';
            showAlert('REST service is offline. Check the connection.', 'error');
            document.getElementById('sendBtn').disabled = true;
        }
    } catch (error) {
        indicator.className = 'status-indicator offline';
        statusText.textContent = '❌ Error';
        statusDetails.textContent = error.message;
        showAlert('Failed to check service status: ' + error.message, 'error');
        document.getElementById('sendBtn').disabled = true;
    }
}

/**
 * Test the API endpoint
 */
async function testAPI() {
    const endpoint = document.getElementById('endpoint').value.trim();
    const responseBox = document.getElementById('responseBox');
    const responseSection = document.getElementById('responseSection');
    const emptyState = document.getElementById('emptyState');
    
    if (!endpoint) {
        showAlert('Please enter an endpoint path', 'warning');
        return;
    }
    
    showAlert('', 'info');
    responseBox.innerHTML = '<div class="spinner"></div>Sending request...';
    responseSection.style.display = 'block';
    emptyState.style.display = 'none';
    
    try {
        const response = await fetch(`${PROXY_URL}?action=request&endpoint=${encodeURIComponent(endpoint)}`);
        const data = await response.json();
        
        if (data.success && data.httpCode >= 200 && data.httpCode < 300) {
            showAlert(`✅ Request successful! (HTTP ${data.httpCode})`, 'success');
            const formatted = JSON.stringify(data.data, null, 2);
            responseBox.innerHTML = syntaxHighlight(formatted);
        } else {
            showAlert(`❌ Error: HTTP ${data.httpCode} - ${data.error}`, 'error');
            responseBox.innerHTML = `<strong>Error:</strong> ${data.error || 'Unknown error'}`;
        }
    } catch (error) {
        showAlert(`❌ Error: ${error.message}`, 'error');
        responseBox.innerHTML = `<strong>Error:</strong> ${error.message}`;
    }
}

/**
 * Syntax highlight JSON with proper formatting
 * @param {string} json - The JSON string to highlight
 * @returns {string} HTML with syntax highlighting applied
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

/**
 * Display alert message to user
 * @param {string} message - The message to display
 * @param {string} type - Alert type: 'success', 'error', 'warning', 'info'
 */
function showAlert(message, type) {
    const alertDiv = document.getElementById('alert');
    alertDiv.innerHTML = message;
    alertDiv.className = 'alert ' + type;
}

/**
 * Copy response to clipboard
 */
function copyResponse() {
    const responseBox = document.getElementById('responseBox');
    const text = responseBox.innerText;
    
    navigator.clipboard.writeText(text).then(() => {
        showAlert('✅ Response copied to clipboard!', 'success');
    }).catch(() => {
        showAlert('❌ Failed to copy response', 'error');
    });
}

/**
 * Clear all results and reset the form
 */
function clearResults() {
    document.getElementById('endpoint').value = '/get';
    document.getElementById('alert').className = 'alert';
    document.getElementById('responseSection').style.display = 'none';
    document.getElementById('emptyState').style.display = 'block';
}
