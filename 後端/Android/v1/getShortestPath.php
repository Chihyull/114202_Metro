<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

require_once '../includes/DbOperations.php';

try {
    // 支援 GET 或 POST；輸入仍為「站點代碼」（不改）
    $start = isset($_REQUEST['start']) ? trim($_REQUEST['start']) : '';
    $end   = isset($_REQUEST['end'])   ? trim($_REQUEST['end'])   : '';

    if ($start === '' || $end === '') {
        http_response_code(400);
        echo json_encode([
            'ok'    => false,
            'error' => 'Missing start or end'
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $db = new DbOperations();

    // 1) 用「距離」為權重計算最短路徑（完全不使用 Time）
    $sp = $db->dijkstraShortestPath($start, $end);

    // 2) 價格與距離：以起迄站查表（你的 station_path 或 view）
    $fare = $db->getFareAndDistance($start, $end);

    // 若路徑找不到，一樣可回傳票價/距離（若資料有）
    if (!$sp['found']) {
        echo json_encode([
            'ok'                 => true,
            'found'              => false,
            'start'              => $start,
            'end'                => $end,
            'path'               => [],
            'full_price'         => $fare ? (int)$fare['FullPrice'] : null,
            'elder_price'        => $fare ? (int)$fare['ElderPrice'] : null,
            'taipei_child_price' => $fare ? (int)$fare['TaipeiChildPrice'] : null,
            'distance'           => $fare ? (float)$fare['Distance'] : null
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // 路徑找到 → 輸出：路徑 + 三種票價 + 距離（不輸出任何時間欄位）
    echo json_encode([
        'ok'                 => true,
        'found'              => true,
        'start'              => $start,
        'end'                => $end,
        'path'               => $sp['path'], // 站碼序列
        'full_price'         => $fare ? (int)$fare['FullPrice'] : null,
        'elder_price'        => $fare ? (int)$fare['ElderPrice'] : null,
        'taipei_child_price' => $fare ? (int)$fare['TaipeiChildPrice'] : null,
        'distance'           => $fare ? (float)$fare['Distance'] : null
    ], JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode([
        'ok'     => false,
        'error'  => 'Server error',
        'detail' => $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
}
