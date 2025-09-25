<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['error' => true, 'message' => 'Invalid request method.'], JSON_UNESCAPED_UNICODE);
    exit;
}

$gmail     = isset($_POST['gmail']) ? trim($_POST['gmail']) : '';
$itsNo     = isset($_POST['its_no']) ? intval($_POST['its_no']) : 0;

/* 可選的更新欄位：沒給就保持原值 */
$title     = isset($_POST['title']) ? trim($_POST['title']) : null;
$startDate = isset($_POST['start_date']) ? trim($_POST['start_date']) : null;
$endDate   = isset($_POST['end_date']) ? trim($_POST['end_date']) : null;
$dest      = isset($_POST['dest']) ? trim($_POST['dest']) : null;

if ($gmail === '' || $itsNo <= 0) {
    echo json_encode(['error' => true, 'message' => 'Missing required parameters. (gmail, its_no)'], JSON_UNESCAPED_UNICODE);
    exit;
}

/* 若四個更新欄位全都沒給，直接回報 */
if ($title === null && $startDate === null && $endDate === null && $dest === null) {
    echo json_encode(['error' => true, 'message' => 'No updatable fields provided.'], JSON_UNESCAPED_UNICODE);
    exit;
}

$db = new DbOperations();

/* 1) 驗證使用者 */
$userNo = $db->getUserNo($gmail);
if ($userNo <= 0) {
    echo json_encode(['error' => true, 'message' => 'User not found or disabled.'], JSON_UNESCAPED_UNICODE);
    exit;
}

/* 2) 取原始行程（同時做擁有者檢查） */
$orig = $db->getItineraryById($itsNo, $userNo);
if (!$orig) {
    echo json_encode(['error' => true, 'message' => 'Itinerary not found or not owned by this user.'], JSON_UNESCAPED_UNICODE);
    exit;
}

/* 3) 用原值補齊沒帶的欄位 */
if ($title === null)     $title     = $orig['Title'];
if ($startDate === null) $startDate = $orig['StartDate'];
if ($endDate === null)   $endDate   = $orig['EndDate'];
if ($dest === null)      $dest      = $orig['Dest'];

/* 4) 基本驗證（日期與站點） */
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

/* 目的地站點檢查（若有變更或沿用原值也要確認存在） */
if (!$db->stationCheck($dest)) {
    echo json_encode(['error' => true, 'message' => 'Destination StationCode not exists: '.$dest], JSON_UNESCAPED_UNICODE);
    exit;
}

/* 5) 更新 */
$ok = $db->updateItinerarySetting($itsNo, $userNo, $title, $startDate, $endDate, $dest);
if ($ok) {
    echo json_encode([
        'error'   => false,
        'message' => 'Itinerary updated successfully.',
        'data'    => [
            'its_no'     => $itsNo,
            'title'      => $title,
            'start_date' => $startDate,
            'end_date'   => $endDate,
            'dest'       => $dest
        ]
    ], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode(['error' => true, 'message' => 'No changes or update failed.'], JSON_UNESCAPED_UNICODE);
}
