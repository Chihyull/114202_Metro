<?php

	class DbOperations{
		
		private $con;

		function __construct(){

			require_once dirname(__FILE__).'/DbConnect.php';

			$db = new DbConnect();

			$this->con = $db->connect();
		}


		/*CRUD -> C -> CREATE */

		function createUser($UserName, $Pass, $Email){
			$Password = md5($Pass);
			$stmt = $this->con->prepare("INSERT INTO `metro`.`users` (`ID`, `UserName`, `Password`, `Email`) 
			VALUES (Null, ?, ?, ?);");
			$stmt->bind_param("sss", $UserName, $Pass, $Email);

			if ($stmt->execute()){
				return true;
			}else{
				return false;
			}
		}
	}