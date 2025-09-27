<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
	echo json_encode(['error'=>true,'message'=>'Invalid request method']); exit;
}

$gmail   = trim($_POST['gmail']   ?? '');
$ulNo    = isset($_POST['ulNo']) ? (int)$_POST['ulNo'] : 0;
$fileName= trim($_POST['fileName'] ?? ''); // 二擇一：優先 ulNo，否則用 fileName

if ($gmail === '' || ($ulNo <= 0 && $fileName === '')) {
	echo json_encode(['error'=>true,'message'=>'Missing: gmail and (ulNo or fileName)']); exit;
}

$db = new DbOperations();

/* 1) 驗證使用者，取得 userNo */
$userNo = $db->getUserNo($gmail);
if ($userNo <= 0) {
	echo json_encode(['error'=>true,'message'=>'Invalid user or user is disabled']); exit;
}

/* 2) 若沒有 ulNo 但有 fileName，先找 ULNo */
if ($ulNo <= 0 && $fileName !== '') {
	if (!method_exists($db, 'getUserLikeByName')) {
		echo json_encode(['error'=>true,'message'=>'getUserLikeByName() not found']); exit;
	}
	$ulNo = (int)$db->getUserLikeByName($userNo, $fileName);
	if ($ulNo <= 0) {
		echo json_encode(['error'=>true,'message'=>'Folder not found by fileName']); exit;
	}
}

/* 3) 刪除資料夾（交易處理：清 0 → 刪 mapping → 刪資料夾） */
if (!method_exists($db, 'deleteUserLike')) {
	echo json_encode(['error'=>true,'message'=>'deleteUserLike() not found']); exit;
}

$ok = $db->deleteUserLike($userNo, $ulNo);

echo json_encode([
	'error'   => !$ok,
	'message' => $ok ? 'Folder deleted' : 'Delete failed',
	'ULNo'    => $ulNo,
	'FileName'=> $fileName ?: null
]);
