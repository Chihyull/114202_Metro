<?php

	class DbOperations{
		
		private $con;

		function __construct(){

			require_once dirname(__FILE__).'/DbConnect.php';

			$db = new DbConnect();

			$this->con = $db->connect();
		}


		/*CRUD -> C -> CREATE */

		public function registerUser($gmail) {
			$stmt = $this->con->prepare("INSERT INTO metro.user_login (UserNo, Gmail, IsStop, CreateTime) VALUES (Null, ?, 'N', NOW())");
		
			$stmt->bind_param("s", $gmail);
		
			if ($stmt->execute()) {
				return 1; // 成功
			} else {
				return 0; // 失敗（可能是已存在或其他錯誤）
			}
		}		
	
	}