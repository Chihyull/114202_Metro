<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['error'=>true,'message'=>'Invalid request method']); exit;
}

$gmail    = trim($_POST['gmail']    ?? '');
$ulNo     = isset($_POST['ulNo']) ? (int)$_POST['ulNo'] : 0;
$fileName = trim($_POST['fileName'] ?? '');

if ($gmail === '' || ($ulNo <= 0 && $fileName === '')) {
    echo json_encode(['error'=>true,'message'=>'Missing: gmail and (ulNo or fileName)']); exit;
}

$db = new DbOperations();
$userNo = $db->getUserNo($gmail);
if ($userNo <= 0) { echo json_encode(['error'=>true,'message'=>'Invalid user or user is disabled']); exit; }

if ($ulNo <= 0 && $fileName !== '') {
    if (!method_exists($db, 'getUserLikeByName')) {
        echo json_encode(['error'=>true,'message'=>'getUserLikeByName() not found']); exit;
    }
    $ulNo = (int)$db->getUserLikeByName($userNo, $fileName);
    if ($ulNo <= 0) { echo json_encode(['error'=>true,'message'=>'Folder not found by fileName']); exit; }
}

/* 檢查歸屬 */
$chk = $db->con->prepare("SELECT 1 FROM metro.user_like WHERE ULNo=? AND UserNo=? LIMIT 1");
if (!$chk) { echo json_encode(['error'=>true,'message'=>'DB prepare failed']); exit; }
$chk->bind_param('ii', $ulNo, $userNo);
$chk->execute();
$res = $chk->get_result();
if ($res->num_rows === 0) {
    echo json_encode(['error'=>true,'message'=>'Folder not found or no permission']); exit;
}

$items = $db->getFavoritesInFolder($ulNo);

echo json_encode([
    'error' => false,
    'message' => 'ok',
    'ULNo' => $ulNo,
    'count' => count($items),
    'items' => $items
]);
