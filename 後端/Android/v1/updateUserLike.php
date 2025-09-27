<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbConnect.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
  echo json_encode(['error'=>true,'message'=>'Invalid request method']); exit;
}

$gmail = trim($_POST['gmail'] ?? '');
$ulNo  = (int)($_POST['ulNo'] ?? 0);
$newName = trim($_POST['fileName'] ?? '');

if ($gmail === '' || $ulNo <= 0 || $newName === '') {
  echo json_encode(['error'=>true,'message'=>'Missing: gmail, ulNo, fileName']); exit;
}

$db = new DbConnect();
$con = $db->connect();

// 1) 取 userNo（必須未停用）
$stmt = $con->prepare("SELECT UserNo FROM metro.user_profile WHERE Gmail=? AND UPPER(IsStop)='N' LIMIT 1");
$stmt->bind_param('s', $gmail);
$stmt->execute();
$res = $stmt->get_result();
if ($res->num_rows === 0) { echo json_encode(['error'=>true,'message'=>'Invalid user']); exit; }
$userNo = (int)$res->fetch_assoc()['UserNo'];

// 2) 確認資料夾屬於此使用者
$stmt = $con->prepare("SELECT 1 FROM metro.user_like WHERE ULNo=? AND UserNo=? LIMIT 1");
$stmt->bind_param('ii', $ulNo, $userNo);
$stmt->execute();
if ($stmt->get_result()->num_rows === 0) {
  echo json_encode(['error'=>true,'message'=>'Folder not found or no permission']); exit;
}

// 3) 檢查同名（同一使用者不可重名）
$stmt = $con->prepare("SELECT 1 FROM metro.user_like WHERE UserNo=? AND FileName=? AND ULNo<>? LIMIT 1");
$stmt->bind_param('isi', $userNo, $newName, $ulNo);
$stmt->execute();
if ($stmt->get_result()->num_rows > 0) {
  echo json_encode(['error'=>true,'message'=>'Folder name already exists']); exit;
}

// 4) 更新名稱
$stmt = $con->prepare("UPDATE metro.user_like SET FileName=?, UpdateTime=NOW() WHERE ULNo=? AND UserNo=?");
$stmt->bind_param('sii', $newName, $ulNo, $userNo);
$ok = $stmt->execute();

echo json_encode([
  'error'=>!$ok,
  'message'=>$ok ? 'Folder updated' : 'Update failed',
  'ULNo'=>$ulNo,
  'FileName'=>$newName
]);
