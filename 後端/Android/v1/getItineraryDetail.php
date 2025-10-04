<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['error' => true, 'message' => 'Invalid request method.'], JSON_UNESCAPED_UNICODE);
    exit;
}

$gmail = trim($_POST['gmail'] ?? '');
$itsNo = intval($_POST['its_no'] ?? 0);

if ($gmail === '' || $itsNo <= 0) {
    echo json_encode(['error' => true, 'message' => 'Missing gmail or its_no'], JSON_UNESCAPED_UNICODE);
    exit;
}

$db = new DbOperations();

/* 1) 取得使用者編號（僅允許有效使用者） */
$userNo = $db->getUserNo($gmail);
if ($userNo <= 0) {
    echo json_encode(['error' => true, 'message' => 'User not found or disabled.'], JSON_UNESCAPED_UNICODE);
    exit;
}

/* 2) 確認行程屬於該使用者 */
$cover = $db->getItineraryById($itsNo, $userNo);
if (!$cover) {
    echo json_encode(['error' => true, 'message' => 'Itinerary not found or not owned by this user.'], JSON_UNESCAPED_UNICODE);
    exit;
}

/* 3) 撈 Day 清單 */
$days = $db->getItineraryDays($itsNo, $userNo);

echo json_encode([
    'error' => false,
    'its_no' => $itsNo,
    'days' => $days  // 形如：[{Day:1, Date:"2025-09-01"}, ...]
], JSON_UNESCAPED_UNICODE);
