<?php

require_once '../includes/DbOperations.php';  // 包含你的資料庫操作類別

header("Content-Type: application/json");
$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    if (isset($_POST['gmail'])) {
        $gmail = $_POST['gmail'];

        $db = new DbOperations();
        $result = $db->registerUser($gmail);

        if ($result == 1) {
            $response['error'] = false;
            $response['message'] = "User registered successfully.";
        } else {
            $response['error'] = true;
            #$response['message'] = "Registration failed or already exists.";
        }
    } else {
        $response['error'] = true;
        $response['message'] = "Required parameter 'gmail' is missing";
    }
} else {
    $response['error'] = true;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>