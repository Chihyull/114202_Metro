<?php

header('Content-Type: application/json');

require_once '../includes/DbOperations.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    if (isset($_POST['gmail'])) {
        $gmail = $_POST['gmail'];

        $db = new DbOperations();

        // 取得名稱的自訂方法 (你需要自己在 DbOperations.php 新增)
        $name = $db->getUserNameByGmail($gmail);

        if ($name !== false) {
            $response['error'] = false;
            $response['UserName'] = $name;
        } else {
            $response['error'] = true;
            $response['message'] = "查無此使用者";
        }
    } else {
        $response['error'] = true;
        $response['message'] = "缺少參數 gmail";
    }
} else {
    $response['error'] = true;
    $response['message'] = "無效的請求方式";
}

echo json_encode($response);
