<?php

	class DbOperations{
		
		private $con;

		function __construct(){

			require_once dirname(__FILE__).'/DbConnect.php';

			$db = new DbConnect();

			$this->con = $db->connect();
		}



		/* get Metro stations */
		public function getStations($lineCode = null) {
			if ($lineCode) {
				$stmt = $this->con->prepare("SELECT StationCode, Station AS NameE, Line, LineCode FROM metro.v_line_station WHERE LineCode = ? ORDER BY StationCode");
				$stmt->bind_param("s", $lineCode);
			} else {
				$stmt = $this->con->prepare("SELECT StationCode, Station AS NameE, Line, LineCode FROM metro.v_line_station ORDER BY StationCode");
			}
		
			$stmt->execute();
			$result = $stmt->get_result();
		
			$stations = [];
			while ($row = $result->fetch_assoc()) {
				$stations[] = $row;
			}
		
			return $stations;
		}
				

		/* create user */
		public function createUser($gmail, $name) {
		// 檢查是否已存在
		$stmt = $this->con->prepare("SELECT UserNo FROM metro.user_profile WHERE Gmail = ?");
		$stmt->bind_param("s", $gmail);
		$stmt->execute();
		$stmt->store_result();

		if ($stmt->num_rows > 0) {
			return 2; // 使用者已存在
		}

		// 尚未存在，插入新資料
		$defaultImage = "default";
		$stmt = $this->con->prepare("
			INSERT INTO metro.user_profile (Gmail, UserName, UserImage, IsStop, CreateTime, UpdateTime)
			VALUES (?, ?, ?, 'N', NOW(), NOW())
		");
		$stmt->bind_param("sss", $gmail, $name, $defaultImage);

		if ($stmt->execute()) {
			return 1; // 新增成功
		} else {
			return 0; // 新增失敗
		}
	}			
	
	}