<?php
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';
require_once '../includes/Constants.php';

/* 用 Constants.php 的 key；只檢查存在且非空 */
if (!defined('GOOGLE_MAPS_API_KEY') || empty(GOOGLE_MAPS_API_KEY)) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'missing_api_key']);
    exit;
}

$radius    = isset($_GET['radius']) ? max(50, min((int)$_GET['radius'], 1500)) : 200;
$perExit   = isset($_GET['perExit']) ? max(1,  min((int)$_GET['perExit'], 60)) : 20;
$startSENo = isset($_GET['startSENo']) ? (int)$_GET['startSENo'] : 0;
$batch     = isset($_GET['batch']) ? max(1, min((int)$_GET['batch'], 500)) : 200;
$type      = isset($_GET['type']) ? trim($_GET['type']) : '';
$keyword   = isset($_GET['keyword']) ? trim($_GET['keyword']) : '';
$dryRun    = isset($_GET['dry_run']) ? (int)$_GET['dry_run'] : 0;

$db = new DbOperations();

/** Nearby：收集 place_id（語系用在地 zh-TW 方便搜尋；不影響後續英文 Details） */
function fetchNearbyPlaceIds($lat, $lng, $radius, $type, $keyword, $want, $apiKey) {
    $base = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    $out = []; $pageToken = null; $page=0;
    do {
        $params = [
            'location' => $lat . ',' . $lng,
            'radius'   => $radius,
            'key'      => $apiKey,
            'language' => 'zh-TW'
        ];
        if ($type !== '')    $params['type']    = $type;
        if ($keyword !== '') $params['keyword'] = $keyword;
        if ($pageToken)      $params['pagetoken'] = $pageToken;

        $url = $base . '?' . http_build_query($params, '', '&', PHP_QUERY_RFC3986);
        $resp = file_get_contents($url);
        if ($resp === false) break;

        $data = json_decode($resp, true);
        if (!is_array($data) || empty($data['status'])) break;
        if ($data['status'] === 'ZERO_RESULTS') break;

        if (!empty($data['results'])) {
            foreach ($data['results'] as $r) {
                if (!empty($r['place_id'])) {
                    $out[] = $r['place_id'];
                    if (count($out) >= $want) break;
                }
            }
        }

        if (count($out) >= $want) break;

        $pageToken = $data['next_page_token'] ?? null;
        if ($pageToken) sleep(2); // 官方要求等待
        $page++;
    } while ($pageToken && $page < 3);

    $out = array_values(array_unique($out));
    if (count($out) > $want) $out = array_slice($out, 0, $want);
    return $out;
}

/** Details：用英文拿 name + 第一張照片的「可直接使用的 URL」 */
function fetchNameEAndPhotoUrl($placeId, $apiKey) {
    $base = "https://maps.googleapis.com/maps/api/place/details/json";
    $params = [
        'place_id' => $placeId,
        'fields'   => 'name,photos',
        'language' => 'en', // 儘量英文，若無則回原語系
        'key'      => $apiKey
    ];
    $url = $base . '?' . http_build_query($params, '', '&', PHP_QUERY_RFC3986);

    $resp = file_get_contents($url);
    $nameE = null;
    $photoUrl = null;

    if ($resp !== false) {
        $data = json_decode($resp, true);
        if (!empty($data['result'])) {
            $res   = $data['result'];
            $nameE = $res['name'] ?? null;

            // 取第一張 photo_reference，並在這裡就組成可用 URL（含金鑰）
            if (!empty($res['photos'][0]['photo_reference'])) {
                $ref = $res['photos'][0]['photo_reference'];
                $photoUrl = 'https://maps.googleapis.com/maps/api/place/photo?maxwidth=400'
                    . '&photo_reference=' . urlencode($ref)
                    . '&key=' . urlencode($apiKey);
            }
        }
    }
    return [$nameE, $photoUrl];
}

$totalExits = 0; $totalUpserts = 0; $lastSENo = $startSENo; $details = [];

while (true) {
    $exits = $db->getExitBatch($lastSENo, $batch);
    if (empty($exits)) break;

    foreach ($exits as $e) {
        $lastSENo = $e['SENo'];
        $totalExits++;

        $pids = fetchNearbyPlaceIds(
            $e['Latitude'], $e['Longitude'],
            $radius, $type, $keyword, $perExit,
            GOOGLE_MAPS_API_KEY
        );

        $w = 0;
        foreach ($pids as $pid) {
            // 一次拿英文名稱 + 圖片 URL（非 photo_reference）
            [$nameE, $photoUrl] = fetchNameEAndPhotoUrl($pid, GOOGLE_MAPS_API_KEY);

            if (!$dryRun) {
                // 仍沿用 upsertStationPlaceFull 的第三參數名稱（欄位叫 PhotoRef 也沒關係，值已經是 URL）
                if ($db->upsertStationPlaceFull($e['SENo'], $pid, $photoUrl, $nameE)) $w++;
            } else {
                $w++;
            }

            // 配額節流，視需要調整
            usleep(120000); // 0.12s
        }

        $totalUpserts += $w;
        usleep(200000); // 出口間節流

        $details[] = [
            'SENo'    => $e['SENo'],
            'found'   => count($pids),
            'written' => $w
        ];
    }

    // 批次間節流
    usleep(300000);
}

echo json_encode([
    'ok'     => true,
    'dry_run'=> (bool)$dryRun,
    'summary'=> [
        'total_exits_processed' => $totalExits,
        'total_places_written'  => $totalUpserts,
        'last_SENo'             => $lastSENo
    ],
    'details'=> $details
], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
