<?php

header("Content-Type: application/json");

require_once '../includes/DbOperations.php'; 

$db = new DbOperations();
$stations = $db->getStations();

echo json_encode($stations);
?>