<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';

$response = [];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // 參數：gmail (必填), limit/offset (選填)
    $gmail  = isset($_POST['gmail']) ? trim($_POST['gmail']) : '';
    $limit  = isset($_POST['limit']) ? (int)$_POST['limit'] : 100;
    $offset = isset($_POST['offset']) ? (int)$_POST['offset'] : 0;

    if ($gmail === '') {
        echo json_encode([
            'error' => true,
            'message' => 'Missing required parameter: gmail.'
        ]);
        exit;
    }

    $db = new DbOperations();
    $userNo = $db->getUserNo($gmail);
    if ($userNo <= 0) {
        echo json_encode([
            'error' => true,
            'message' => 'Invalid user or user is disabled.'
        ]);
        exit;
    }

    // 查數量 + 查清單（前端可用 count 判斷 empty 畫面）
    $total = $db->countUserLikes($userNo);
    $items = $total > 0 ? $db->getUserLikes($userNo, $limit, $offset) : [];

    echo json_encode([
        'error'   => false,
        'message' => 'ok',
        'count'   => $total,
        'items'   => $items,
        'limit'   => $limit,
        'offset'  => $offset,
        'UserNo'  => $userNo,
    ]);
    exit;

} else {
    echo json_encode([
        'error' => true,
        'message' => 'Invalid request method.'
    ]);
    exit;
}
