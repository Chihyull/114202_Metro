<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';
require_once '../includes/Constants.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode(['ok' => false, 'error' => 'invalid_method']);
    exit;
}
if (!defined('GOOGLE_MAPS_API_KEY') || GOOGLE_MAPS_API_KEY === 'YOUR_GOOGLE_MAPS_API_KEY') {
    echo json_encode(['ok' => false, 'error' => 'missing_api_key']);
    exit;
}

$db = new DbOperations();

// 參數：優先用 seno；若無，則用 lat+lng
$seno     = isset($_GET['seno']) ? (int)$_GET['seno'] : 0;
$latParam = isset($_GET['lat']) ? trim($_GET['lat']) : null;
$lngParam = isset($_GET['lng']) ? trim($_GET['lng']) : null;

// Places 搜尋半徑與條件
$radius  = isset($_GET['radius']) ? (int)$_GET['radius'] : 200;
$radius  = max(50, min($radius, 1500)); // 限制 50~1500m
$keyword = isset($_GET['keyword']) ? trim($_GET['keyword']) : '';
$type    = isset($_GET['type']) ? trim($_GET['type']) : '';
$limit   = isset($_GET['limit']) ? max(1, (int)$_GET['limit']) : 20;

// 吸附（反查最近出口）的最大距離，超過就視為找不到對應出口
$snapMax = isset($_GET['snap_max']) ? (int)$_GET['snap_max'] : 200; // 預設 200m
$snapMax = max(50, min($snapMax, 1000));

$exit = null;

// A) 有帶 seno：直接用
if ($seno > 0) {
    $exit = $db->getExitBySENo($seno);
    if (!$exit) {
        echo json_encode(['ok' => false, 'error' => 'not_found', 'msg' => 'SENo not found']);
        exit;
    }
// B) 沒有 seno，但有 lat/lng：先反查最近出口
} elseif ($latParam !== null && $lngParam !== null && $latParam !== '' && $lngParam !== '') {
    if (!is_numeric($latParam) || !is_numeric($lngParam)) {
        echo json_encode(['ok' => false, 'error' => 'bad_param', 'msg' => 'lat/lng must be numeric']);
        exit;
    }
    $latIn = (float)$latParam;
    $lngIn = (float)$lngParam;

    $nearest = $db->getNearestExitByLatLng($latIn, $lngIn);
    if (!$nearest) {
        echo json_encode(['ok' => false, 'error' => 'no_nearby_exit', 'msg' => 'no exit found']);
        exit;
    }
    // 距離過遠就不要硬配
    if ($nearest['distance_m'] > $snapMax) {
        echo json_encode([
            'ok' => false,
            'error' => 'no_nearby_exit',
            'msg' => 'nearest exit beyond snap_max',
            'nearest_distance_m' => $nearest['distance_m']
        ]);
        exit;
    }
    // 視同找到了對應的出口，之後流程與 seno 相同
    $exit = $nearest;
// C) 兩者都沒有
} else {
    echo json_encode(['ok' => false, 'error' => 'missing_param', 'msg' => 'need seno or lat+lng']);
    exit;
}

$lat = $exit['Latitude'];
$lng = $exit['Longitude'];

// 呼叫 Google Places Nearby Search
$baseUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
$params = [
    'location' => $lat . ',' . $lng,
    'radius'   => $radius,
    'key'      => GOOGLE_MAPS_API_KEY,
    'language' => 'zh-TW'
];
if ($keyword !== '') $params['keyword'] = $keyword;
if ($type    !== '') $params['type']    = $type;

$url = $baseUrl . '?' . http_build_query($params, '', '&', PHP_QUERY_RFC3986);

// cURL
$ch = curl_init();
curl_setopt_array($ch, [
    CURLOPT_URL => $url,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_CONNECTTIMEOUT => 6,
    CURLOPT_TIMEOUT => 12,
    CURLOPT_SSL_VERIFYPEER => true,
]);
$apiResp  = curl_exec($ch);
$curlErr  = curl_error($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($apiResp === false || $httpCode !== 200) {
    echo json_encode(['ok' => false, 'error' => 'api_failed', 'msg' => $curlErr ?: ('HTTP ' . $httpCode)]);
    exit;
}

$data = json_decode($apiResp, true);
if (!is_array($data) || !isset($data['status'])) {
    echo json_encode(['ok' => false, 'error' => 'bad_response']);
    exit;
}
if (!in_array($data['status'], ['OK', 'ZERO_RESULTS'])) {
    echo json_encode([
        'ok' => false,
        'error' => 'google_status',
        'status' => $data['status'],
        'msg' => isset($data['error_message']) ? $data['error_message'] : null
    ]);
    exit;
}

$results = $data['results'] ?? [];
$results = array_slice($results, 0, $limit);

// 寫入 station_place（只有找到出口時才寫；用 lat/lng 反查時也會有 SENo）
$items = [];
foreach ($results as $r) {
    $placeId = $r['place_id'] ?? null;
    if (!$placeId) continue;

    $db->upsertStationPlace((int)$exit['SENo'], $placeId);

    $photoUrl = null;
    if (!empty($r['photos'][0]['photo_reference'])) {
        $photoUrl = 'https://maps.googleapis.com/maps/api/place/photo?' . http_build_query([
            'maxwidth'        => 400,
            'photo_reference' => $r['photos'][0]['photo_reference'],
            'key'             => GOOGLE_MAPS_API_KEY
        ], '', '&', PHP_QUERY_RFC3986);
    }

    $items[] = [
        'place_id'  => $placeId,
        'name'      => $r['name']  ?? '',
        'photo_url' => $photoUrl,
        'types'     => $r['types'] ?? []
    ];
}

// 回傳
$out = [
    'ok'          => true,
    'seno'        => $exit['SENo'],
    'exit'        => $exit['Exit'],
    'stationCode' => $exit['StationCode'],
    'location'    => ['lat' => $lat, 'lng' => $lng],
    'count'       => count($items),
    'items'       => $items
];

// 若是用 lat/lng 反查，順便告知與最近出口的距離
if (isset($exit['distance_m'])) {
    $out['snap_info'] = [
        'nearest_distance_m' => $exit['distance_m'],
        'snap_max'           => $snapMax
    ];
}

echo json_encode($out, JSON_UNESCAPED_UNICODE);
