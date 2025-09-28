<?php

header('Content-Type: application/json; charset=utf-8');

try {
    require_once '../includes/DbOperations.php';

    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        echo json_encode(['error'=>true,'message'=>'Invalid request method']); exit;
    }

    $gmail    = trim($_POST['gmail']    ?? '');
    $ulNo     = isset($_POST['ulNo']) ? (int)$_POST['ulNo'] : 0;
    $fileName = trim($_POST['fileName'] ?? '');
    $spNo     = isset($_POST['spNo']) ? (int)$_POST['spNo'] : 0;

    if ($gmail === '' || $spNo <= 0 || ($ulNo <= 0 && $fileName === '')) {
        echo json_encode(['error'=>true,'message'=>'Missing: gmail, spNo, and (ulNo or fileName)']); exit;
    }

    $db = new DbOperations();

    // 1) 驗證使用者
    $userNo = $db->getUserNo($gmail);
    if ($userNo <= 0) {
        echo json_encode(['error'=>true,'message'=>'Invalid user or user is disabled']); exit;
    }

    // 2) 補查 ulNo（優先用 fileName 對應）
    if ($ulNo <= 0 && $fileName !== '') {
        $ulNo = (int)$db->getUserLikeByName($userNo, $fileName);
        if ($ulNo <= 0) {
            // 如果要自動建立資料夾，解除下行註解：
            // $res = $db->createUserLike($userNo, $fileName);
            // if ($res['status'] === 'created' || $res['status'] === 'exists') $ulNo = (int)$res['ULNo'];

            if ($ulNo <= 0) {
                echo json_encode(['error'=>true,'message'=>'Folder not found by fileName']); exit;
            }
        }
    }

    // 3) 檢查資料夾歸屬（封裝在 DbOperations）
    if (!$db->folderOwnedBy($ulNo, $userNo)) {
        echo json_encode(['error'=>true,'message'=>'Folder not found or no permission']); exit;
    }

    // 4) 設為收藏
    $ok = $db->setFavorite($ulNo, $spNo);

    echo json_encode([
        'error'      => !$ok,
        'message'    => $ok ? 'Favorite set' : 'Set favorite failed',
        'ULNo'       => $ulNo,
        'SPNo'       => $spNo,
        'IsFavorite' => 1
    ]);
} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(['error'=>true,'message'=>'Server error: '.$e->getMessage()]);
}
