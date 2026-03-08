<?php
// proxy.php - Acts as a bridge between browser and REST service

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Configuration - Use IP address instead of container name
$REST_SERVICE_HOST = '172.32.0.11';  // Your service IP
$REST_SERVICE_PORT = 32768;
$REST_SERVICE_URL = "http://{$REST_SERVICE_HOST}:{$REST_SERVICE_PORT}";
$TIMEOUT = 5;

/**
 * Check if REST service is reachable
 */
function checkServiceStatus() {
    global $REST_SERVICE_URL, $TIMEOUT;
    
    $ch = curl_init($REST_SERVICE_URL . '/api');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, $TIMEOUT);
    curl_setopt($ch, CURLOPT_NOBODY, true);  // HEAD request
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, $TIMEOUT);
    
    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    
    return [
        'status' => ($result !== false && $httpCode > 0) ? 'online' : 'offline',
        'httpCode' => $httpCode,
        'error' => $error,
        'url' => $REST_SERVICE_URL,
        'timestamp' => date('Y-m-d H:i:s')
    ];
}

/**
 * Forward request to REST service
 */
function forwardRequest($endpoint, $method) {
    global $REST_SERVICE_URL, $TIMEOUT;
    
    $apiUrl = $REST_SERVICE_URL . '/api' . $endpoint;
    
    $ch = curl_init($apiUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_TIMEOUT, $TIMEOUT);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, $TIMEOUT);
    
    // Forward any request body (for POST, PUT, etc.)
    if (in_array($method, ['POST', 'PUT', 'PATCH'])) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, file_get_contents('php://input'));
    }
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    
    return [
        'success' => $response !== false,
        'httpCode' => $httpCode,
        'data' => $response ? json_decode($response, true) : null,
        'error' => $error,
        'timestamp' => date('Y-m-d H:i:s')
    ];
}

// Route the request
$action = isset($_GET['action']) ? $_GET['action'] : 'request';

try {
    if ($action === 'status') {
        // Check service status
        $status = checkServiceStatus();
        http_response_code(200);
        echo json_encode($status);
        
    } else if ($action === 'request') {
        // Forward API request
        $endpoint = isset($_GET['endpoint']) ? $_GET['endpoint'] : '/get';
        $method = $_SERVER['REQUEST_METHOD'];
        
        $result = forwardRequest($endpoint, $method);
        http_response_code($result['httpCode']);
        
        echo json_encode($result);
        
    } else {
        http_response_code(400);
        echo json_encode(['error' => 'Unknown action']);
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'error' => $e->getMessage(),
        'timestamp' => date('Y-m-d H:i:s')
    ]);
}
?>
