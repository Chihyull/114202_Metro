<?php

header("Content-Type: application/json");

require_once '../includes/DbOperations.php'; 

$db = new DbOperations();

// 從 URL 或 POST 接收 lineCode 參數，沒有就是 null
$lineCode = isset($_GET['lineCode']) ? $_GET['lineCode'] : null;

$stations = $db->getStations($lineCode);

echo json_encode($stations);
