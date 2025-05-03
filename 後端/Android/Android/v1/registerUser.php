
<?php

require_once '../includes/DbOperations.php';
$response = array(); 

if ($_SERVER['REQUEST_METHOD']=='POST'){
    if(
        isset($_POST['Username']) and
        isset($_POST['Password']) and 
        isset($_POST['Email'])
        ){
        //operate the data further
        $db = new DbOperations();
        $db = new DbOperations();

        if($db->createUser(
            $_POST['Username'],
            $_POST['Password'],
            $_POST['Email']
            )){
                $response['error'] = false;
                $response['message'] = "User registered successfully";
            }else{
                $response['error'] = true;
                $response['message'] = "Some error occurred please try again";                
            }

       }else{
        $response['error'] = true;
        $response['message'] = "Required fields are missing";
       }
}else{
    $response['error'] = true;
    $response['message'] = "Invalid Request";
}

echo json_encode($response);