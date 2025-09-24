<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

require_once '../includes/DbOperations.php'; 

try {
    // 支援 GET 或 POST
    $start = isset($_REQUEST['start']) ? trim($_REQUEST['start']) : '';
    $end   = isset($_REQUEST['end'])   ? trim($_REQUEST['end'])   : '';

    if ($start === '' || $end === '') {
        http_response_code(400);
        echo json_encode([
            'ok' => false,
            'error' => 'Missing start or end'
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $db = new DbOperations();
    $result = $db->dijkstraShortestPath($start, $end);

    if (!$result['found']) {
        echo json_encode([
            'ok' => true,
            'found' => false,
            'start' => $start,
            'end' => $end,
            'total_time' => null,
            'path' => []
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    echo json_encode([
        'ok' => true,
        'found' => true,
        'start' => $start,
        'end' => $end,
        'total_time' => (int)$result['distance'], // 單位=與 station_path.Time 相同
        'path' => $result['path']                 // 節點序列（站碼或站名，視你表格）
    ], JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode([
        'ok' => false,
        'error' => 'Server error',
        'detail' => $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
}
