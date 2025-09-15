#updateUserName.php
<?php

header('Content-Type: application/json');

require_once '../includes/DbOperations.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    if (isset($_POST['gmail']) && isset($_POST['name'])) {

        $db = new DbOperations();

        $result = $db->updateUserName($_POST['gmail'], $_POST['name']);

        if ($result) {
            $response['error'] = false;
            $response['message'] = "User name updated successfully";
            $response['updated_name'] = $_POST['name'];  // 回傳更新的名稱
        } else {
            $response['error'] = true;
            $response['message'] = "Failed to update user name";
        }

    } else {
        $response['error'] = true;
        $response['message'] = "Required fields are missing";
    }
} else {
    $response['error'] = true;
    $response['message'] = "Invalid Request";
}

echo json_encode($response);