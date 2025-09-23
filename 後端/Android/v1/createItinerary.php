<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['error' => true, 'message' => 'Invalid request method.'], JSON_UNESCAPED_UNICODE);
    exit;
}

$gmail     = isset($_POST['gmail']) ? trim($_POST['gmail']) : '';
$title     = isset($_POST['title']) ? trim($_POST['title']) : '';
$startDate = isset($_POST['start_date']) ? trim($_POST['start_date']) : '';
$endDate   = isset($_POST['end_date']) ? trim($_POST['end_date']) : '';
$dest      = isset($_POST['dest']) ? trim($_POST['dest']) : '';

if ($gmail === '' || $title === '' || $startDate === '' || $endDate === '' || $dest === '') {
    echo json_encode(['error' => true, 'message' => 'Missing required parameters. (gmail, title, start_date, end_date, dest)'], JSON_UNESCAPED_UNICODE);
    exit;
}

/* 簡單日期檢查 */
$dtStart = DateTime::createFromFormat('Y-m-d', $startDate);
$dtEnd   = DateTime::createFromFormat('Y-m-d', $endDate);
if (!$dtStart || $dtStart->format('Y-m-d') !== $startDate ||
    !$dtEnd   || $dtEnd->format('Y-m-d') !== $endDate) {
    echo json_encode(['error' => true, 'message' => 'start_date / end_date must be YYYY-MM-DD'], JSON_UNESCAPED_UNICODE);
    exit;
}
if ($endDate < $startDate) {
    echo json_encode(['error' => true, 'message' => 'end_date cannot be earlier than start_date'], JSON_UNESCAPED_UNICODE);
    exit;
}

$db = new DbOperations();

/* 1) 驗證使用者 */
$userNo = $db->getUserNo($gmail);
if ($userNo <= 0) {
    echo json_encode(['error' => true, 'message' => 'User not found or disabled.'], JSON_UNESCAPED_UNICODE);
    exit;
}

/* 2) 驗證目的地站點 */
if (!$db->stationCheck($dest)) {
    echo json_encode(['error' => true, 'message' => 'Destination StationCode not exists: '.$dest], JSON_UNESCAPED_UNICODE);
    exit;
}

/* 3) 建立行程 */
$newId = $db->createItinerarySetting($userNo, $title, $startDate, $endDate, $dest);
if ($newId > 0) {
    echo json_encode(['error' => false, 'itsNo' => $newId, 'message' => 'Itinerary created successfully.'], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode(['error' => true, 'message' => 'Failed to create itinerary.'], JSON_UNESCAPED_UNICODE);
}
