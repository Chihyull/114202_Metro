<?php
header("Content-Type: application/json; charset=utf-8");

require_once '../includes/DbOperations.php';

try {
    $db = new DbOperations();

    // 可選參數：?stationCode=BL12
    $stationCode = isset($_GET['stationCode']) ? trim($_GET['stationCode']) : null;

    $data = $db->getStationExits($stationCode);

    echo json_encode([
        'ok'   => true,
        'data' => $data
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);

} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode([
        'ok'    => false,
        'error' => 'server_error',
        'msg'   => $e->getMessage()
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
}
