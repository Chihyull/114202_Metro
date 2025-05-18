<?php 
header('Content-Type: application/json; charset=utf-8');
require_once '../includes/DbOperations.php';

$response = array(); 

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    if (isset($_POST['gmail']) && isset($_POST['name'])) {
        $gmail = $_POST['gmail'];
        $username = $_POST['name'];

        $db = new DbOperations(); 

        $result = $db->createUser($gmail);

        if ($result == 1) {
            // 新使用者建立 profile（圖片固定 default）
            $profileResult = $db->createProfile($gmail, $username);
            if ($profileResult == 1) {
                $response['error'] = false; 
                $response['message'] = "User registered and profile created.";
            } else {
                $response['error'] = true; 
                $response['message'] = "User created but profile creation failed.";
            }
        } elseif ($result == 2) {
            $response['error'] = false; 
            #$response['message'] = "User already exists.";
        } else {
            $response['error'] = true; 
            $response['message'] = "User registration failed.";
        }
    } else {
        $response['error'] = true; 
        $response['message'] = "Missing required parameters.";
    }
} else {
    $response['error'] = true; 
    $response['message'] = "Invalid request method.";
}

echo json_encode($response);
?>
