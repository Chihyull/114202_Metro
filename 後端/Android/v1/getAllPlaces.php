<?php
/**
 * 一次性批次：把所有出口附近的 place_id 抓下來，寫進 metro.station_place
 * 路徑示例：
 *   http://<host>/metro/api/seedAllExitPlaces.php
 * 可選參數：
 *   radius=200           // Places 搜尋半徑（公尺，50~1500）
 *   perExit=20           // 每個出口最多收幾筆（1~60）
 *   startSENo=0          // 從哪個 SENo 開始（續跑用）
 *   batch=200            // 每批抓幾個出口（1~500）
 *   type=restaurant      // Places type（可留空）
 *   keyword=xxx          // Places keyword（可留空）
 *   dry_run=1            // 試跑不寫 DB（預設 0）
 *   token=YOUR_SECRET    // （可選）簡單保護；不提供就不檢查
 */
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';
require_once '../includes/Constants.php';

if (!defined('GOOGLE_MAPS_API_KEY') || GOOGLE_MAPS_API_KEY === 'YOUR_GOOGLE_MAPS_API_KEY') {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'missing_api_key']);
    exit;
}

// 可選：非常簡單的保護（要就填，否則略過）
$wantToken = ''; // 例如 'abc123'；若留空就不檢查
if ($wantToken !== '') {
    $token = isset($_GET['token']) ? $_GET['token'] : '';
    if ($token !== $wantToken) {
        http_response_code(403);
        echo json_encode(['ok' => false, 'error' => 'forbidden']);
        exit;
    }
}

// 參數
$radius   = isset($_GET['radius']) ? (int)$_GET['radius'] : 200;
$radius   = max(50, min($radius, 1500));
$perExit  = isset($_GET['perExit']) ? (int)$_GET['perExit'] : 20;
$perExit  = max(1, min($perExit, 60));  // Nearby Search 一頁最多 20，含翻頁最多 60
$startSENo= isset($_GET['startSENo']) ? (int)$_GET['startSENo'] : 0;
$batch    = isset($_GET['batch']) ? (int)$_GET['batch'] : 200;
$batch    = max(1, min($batch, 500));
$type     = isset($_GET['type']) ? trim($_GET['type']) : '';
$keyword  = isset($_GET['keyword']) ? trim($_GET['keyword']) : '';
$dryRun   = isset($_GET['dry_run']) ? (int)$_GET['dry_run'] : 0;

$db = new DbOperations();

// 工具：打 Google Nearby Search（含 next_page_token 翻頁）
function fetchNearbyPlaceIds($lat, $lng, $radius, $type, $keyword, $want, $apiKey) {
    $base = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    $collected = [];
    $pageToken = null;
    $page = 0;

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

        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CONNECTTIMEOUT => 8,
            CURLOPT_TIMEOUT => 15,
            CURLOPT_SSL_VERIFYPEER => true,
        ]);
        $resp = curl_exec($ch);
        $err  = curl_error($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($resp === false || $code !== 200) {
            break; // 中止這個出口，避免死循環
        }

        $data = json_decode($resp, true);
        if (!is_array($data) || !isset($data['status'])) {
            break;
        }
        if ($data['status'] === 'ZERO_RESULTS') {
            break;
        }
        if ($data['status'] !== 'OK' && $data['status'] !== 'INVALID_REQUEST') {
            // 可能是 OVER_QUERY_LIMIT, REQUEST_DENIED 等；直接中止
            break;
        }

        // 收集本頁
        if (!empty($data['results'])) {
            foreach ($data['results'] as $r) {
                if (!empty($r['place_id'])) {
                    $collected[] = $r['place_id'];
                    if (count($collected) >= $want) break;
                }
            }
        }

        // 判斷是否還要下一頁
        if (count($collected) >= $want) break;

        $pageToken = isset($data['next_page_token']) ? $data['next_page_token'] : null;
        if ($pageToken) {
            // 官方要求等待 2 秒以上才會生效
            sleep(2);
        }
        $page++;
    } while ($pageToken && $page < 3); // Nearby 最多 3 頁

    // 去重
    $collected = array_values(array_unique($collected));
    // 截斷
    if (count($collected) > $want) {
        $collected = array_slice($collected, 0, $want);
    }
    return $collected;
}

// 主循環：分批抓出口→打 API→寫入 DB
$totalExits = 0;
$totalPlacesInserted = 0;
$lastSENo = $startSENo;
$exitStats = [];

while (true) {
    $exits = $db->getExitBatch($lastSENo, $batch);
    if (empty($exits)) break;

    foreach ($exits as $e) {
        $lastSENo = $e['SENo'];
        $totalExits++;

        $lat = $e['Latitude'];
        $lng = $e['Longitude'];

        $placeIds = fetchNearbyPlaceIds($lat, $lng, $radius, $type, $keyword, $perExit, GOOGLE_MAPS_API_KEY);

        $insCount = 0;
        if (!$dryRun) {
            foreach ($placeIds as $pid) {
                if ($db->upsertStationPlace($e['SENo'], $pid)) {
                    $insCount++;
                }
            }
        } else {
            $insCount = count($placeIds); // 乾跑統計用
        }

        $totalPlacesInserted += $insCount;

        // 可選：簡單節流，避免過快（視配額調整）
        usleep(300000); // 0.3s

        // 每個出口的摘要（可關掉以減少輸出）
        $exitStats[] = [
            'SENo'      => $e['SENo'],
            'station'   => $e['StationCode'],
            'exit'      => $e['Exit'],
            'lat'       => $lat,
            'lng'       => $lng,
            'found'     => count($placeIds),
            'inserted'  => $insCount,
        ];
    }
    // 可選：每批之間休息一下
    usleep(300000); // 0.3s
}

// 輸出
echo json_encode([
    'ok' => true,
    'dry_run' => (bool)$dryRun,
    'params' => [
        'radius'     => $radius,
        'perExit'    => $perExit,
        'startSENo'  => $startSENo,
        'batch'      => $batch,
        'type'       => $type,
        'keyword'    => $keyword
    ],
    'summary' => [
        'total_exits_processed' => $totalExits,
        'total_places_written'  => $totalPlacesInserted,
        'last_SENo'             => $lastSENo
    ],
    'details' => $exitStats
], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
