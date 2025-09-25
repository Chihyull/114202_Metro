<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';

$response = [];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {

    // 必填參數：用 Gmail 找 UserNo，再用 FileName 建立資料夾
    if (isset($_POST['gmail']) && isset($_POST['fileName'])) {
        $gmail    = trim($_POST['gmail']);
        $fileName = trim($_POST['fileName']);

        // 簡單驗證
        if ($fileName === '') {
            echo json_encode([
                'error' => true,
                'message' => 'Folder name cannot be empty.'
            ]);
            exit;
        }

        $db = new DbOperations();

        // 取得有效的使用者編號（未停用）
        $userNo = $db->getUserNo($gmail);
        if ($userNo <= 0) {
            echo json_encode([
                'error' => true,
                'message' => 'Invalid user or user is disabled.'
            ]);
            exit;
        }

        // 建立資料夾（內含同名檢查）
        $result = $db->createUserLike($userNo, $fileName);
        // $result => ['status' => 'created'|'exists'|'error', 'ULNo' => int]

        if ($result['status'] === 'created') {
            $response['error']   = false;
            $response['message'] = 'Folder created.';
            $response['status']  = 'created';
            $response['ULNo']    = $result['ULNo'];
            $response['FileName']= $fileName;
            $response['UserNo']  = $userNo;
        } elseif ($result['status'] === 'exists') {
            // 同名資料夾已存在：不視為錯誤，回傳既有 ULNo
            $response['error']   = false;
            $response['message'] = 'Folder already exists.';
            $response['status']  = 'exists';
            $response['ULNo']    = $result['ULNo'];
            $response['FileName']= $fileName;
            $response['UserNo']  = $userNo;
        } else {
            $response['error']   = true;
            $response['message'] = 'Create folder failed.';
            $response['status']  = 'error';
        }

    } else {
        $response['error'] = true;
        $response['message'] = 'Missing required parameters: gmail, fileName.';
    }

} else {
    $response['error'] = true;
    $response['message'] = 'Invalid request method.';
}

echo json_encode($response);
