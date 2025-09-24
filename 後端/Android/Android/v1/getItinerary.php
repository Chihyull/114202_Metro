<?php 
header('Content-Type: application/json; charset=utf-8');

require_once '../includes/DbOperations.php';

$response = array(); 

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    if (isset($_POST['gmail'])) {
        $gmail = trim($_POST['gmail']);

        $db = new DbOperations(); 

        // 1) 取得 UserNo（僅允許 IsStop = 'N' 的使用者）
        $userNo = $db->getUserNo($gmail);
        if ($userNo <= 0) {
            $response['error'] = true; 
            $response['message'] = "User not found or disabled.";
            echo json_encode($response, JSON_UNESCAPED_UNICODE);
            exit;
        }

        // 2) 取得該使用者所有行程（依 ITSNo DESC）
        $items = $db->getItinerary($userNo);

        $response['error'] = false; 
        $response['count'] = count($items);
        $response['items'] = $items; // 每筆包含：ITSNo, Title, StartDate, EndDate, Dest
    } else {
        $response['error'] = true; 
        $response['message'] = "Missing required parameters.";
    }
} else {
    $response['error'] = true; 
    $response['message'] = "Invalid request method.";
}

echo json_encode($response, JSON_UNESCAPED_UNICODE);
?>
