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
				

		/* user register */
		public function createUser($gmail) {
			// 檢查是否已存在
			$stmt = $this->con->prepare("SELECT UserNo FROM metro.user_login WHERE Gmail = ?");
			$stmt->bind_param("s", $gmail);
			$stmt->execute();
			$stmt->store_result();
		
			if ($stmt->num_rows > 0) {
				return 2; // 已存在
			}
		
			// 尚未存在，插入新資料
			$stmt = $this->con->prepare("
				INSERT INTO metro.user_login (Gmail, IsStop, CreateTime) 
				VALUES (?, 'N', NOW())
			");
			$stmt->bind_param("s", $gmail);
		
			if ($stmt->execute()) {
				return 1; // 新增成功
			} else {
				return 0; // 失敗
			}
		}
		
		public function createProfile($gmail, $name) {
			// 查找 UserNo
			$stmt = $this->con->prepare("SELECT UserNo FROM metro.user_login WHERE Gmail = ?");
			$stmt->bind_param("s", $gmail);
			$stmt->execute();
			$stmt->bind_result($userNo);
		
			if ($stmt->fetch()) {
				$stmt->close();
		
				// 圖片強制設為 default
				$defaultImage = "default";
				$stmt = $this->con->prepare("
					INSERT INTO metro.user_profile (UserNo, UserName, UserImage, UpdateTime) 
					VALUES (?, ?, ?, NOW())
				");
				$stmt->bind_param("iss", $userNo, $name, $defaultImage);
				if ($stmt->execute()) {
					return 1;
				}
			}
		
			return 0;
		}				
	
	}