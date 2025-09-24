<?php

	class DbOperations{
		
		private $con;

		function __construct(){

			require_once dirname(__FILE__).'/DbConnect.php';

			$db = new DbConnect();

			$this->con = $db->connect();
		}

		
		/* 取單筆行程並同時確認擁有者 */
		public function getItineraryById($itsNo, $userNo) {
			$sql = "SELECT ITSNo, UserNo, Title, StartDate, EndDate, Dest
					FROM metro.itinerary_setting
					WHERE ITSNo = ? AND UserNo = ?
					LIMIT 1";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return null;
			$stmt->bind_param("ii", $itsNo, $userNo);
			$stmt->execute();
			$res = $stmt->get_result();
			if ($row = $res->fetch_assoc()) {
				return $row;
			}
			return null;
		}

		/* 更新行程 */
		public function updateItinerarySetting($itsNo, $userNo, $title, $startDate, $endDate, $dest) {
			$sql = "UPDATE metro.itinerary_setting
					SET Title = ?, StartDate = ?, EndDate = ?, Dest = ?, UpdateTime = NOW()
					WHERE ITSNo = ? AND UserNo = ?";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return false;
			$stmt->bind_param("ssssii", $title, $startDate, $endDate, $dest, $itsNo, $userNo);
			if (!$stmt->execute()) return false;
			/* 受影響列可能是 0（資料相同），這裡仍回 true 表示請求成功 */
			return true;
		}

		/* 新增行程 */
		public function createItinerarySetting($userNo, $title, $startDate, $endDate, $dest) {
			$sql = "INSERT INTO metro.itinerary_setting
					(UserNo, Title, StartDate, EndDate, Dest, Cover, CreateTime, UpdateTime)
					VALUES (?, ?, ?, ?, ?, NULL, NOW(), NOW())";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return 0;
			$stmt->bind_param("issss", $userNo, $title, $startDate, $endDate, $dest);
			if (!$stmt->execute()) return 0;
			return (int)$stmt->insert_id;
		}

		/* 查看行程數量 */
		public function getItinerary($userNo) {

			$sql = "SELECT ITSNo, Title, StartDate, EndDate, Dest
					FROM metro.itinerary_setting
					WHERE UserNo = ?
					ORDER BY ITSNo DESC";

			$stmt = $this->con->prepare($sql);
			if (!$stmt) return [];
			$stmt->bind_param("i", $userNo);
			$stmt->execute();
			$res = $stmt->get_result();

			$items = [];
			while ($row = $res->fetch_assoc()) {
				$items[] = [
					'ITSNo'     => (int)$row['ITSNo'],
					'Title'     => $row['Title'],
					'StartDate' => $row['StartDate'],
					'EndDate'   => $row['EndDate'],
					'Dest'      => $row['Dest']
				];
			}
			return $items;
		}

		/* 透過UserNo進行驗證 */
		public function getUserNo($gmail) {
			$sql = "SELECT UserNo, IsStop FROM metro.user_profile WHERE Gmail = ? LIMIT 1";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return -1;
			$stmt->bind_param("s", $gmail);
			$stmt->execute();
			$res = $stmt->get_result();
			if ($res->num_rows === 0) return -1;
			$row = $res->fetch_assoc();
			if (strtoupper($row['IsStop']) !== 'N') return -1;
			return (int)$row['UserNo'];
		}

		/* 檢查站點代碼 */
		public function stationCheck($stationCode) {
			$sql = "SELECT COUNT(*) AS cnt FROM metro.station WHERE StationCode = ? LIMIT 1";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return false;
			$stmt->bind_param("s", $stationCode);
			$stmt->execute();
			$res = $stmt->get_result();
			if ($row = $res->fetch_assoc()) {
				return $row['cnt'] > 0;
			}
			return false;
		}

		/* 獲取捷運出口資料 */
		public function getStationExits($stationCode = null) {
			if ($stationCode) {
				$stmt = $this->con->prepare("
					SELECT StationCode, nameE, `Exit` AS ExitCode
					FROM metro.v_station_exit_place
					WHERE StationCode = ?
					ORDER BY `Exit`
				");
				$stmt->bind_param("s", $stationCode);
			} else {
				$stmt = $this->con->prepare("
					SELECT StationCode, nameE, `Exit` AS ExitCode
					FROM metro.v_station_exit_place
					ORDER BY StationCode, `Exit`
				");
			}
			$stmt->execute();
			$result = $stmt->get_result();

			$rows = [];
			while ($row = $result->fetch_assoc()) {
				// 統一輸出欄位命名：StationCode, NameE, ExitCode
				$rows[] = [
					'StationCode' => $row['StationCode'],
					'NameE'       => $row['nameE'],
					'ExitCode'    => $row['ExitCode'],
				];
			}
			return $rows;
		}

		/* 查找最短路徑 */
		/* 取得整張圖（一次載入）：key = Start，value = 陣列( [neighbor => time] ) */
		public function buildGraph() {
			$sql = "SELECT Start, End, Time FROM metro.station_path";
			$res = $this->con->query($sql);

			$graph = [];
			while ($row = $res->fetch_assoc()) {
				$u = $row['Start'];
				$v = $row['End'];
				$w = (int)$row['Time'];

				if (!isset($graph[$u])) $graph[$u] = [];
				$graph[$u][$v] = $w;

				/* 如果你的邊是「無向」的，打開下面這行，會自動補反向邊 */
				if (!isset($graph[$v])) $graph[$v] = [];
				$graph[$v][$u] = $w;
			}
			return $graph;
		}

		/* Dijkstra：回傳最短距離與路徑 */
		public function dijkstraShortestPath($start, $end) {
			$graph = $this->buildGraph();

			if (!isset($graph[$start]) && !array_key_exists($start, $graph)) {
				// 起點沒有任何出邊且不在 key 中
				// 有些點可能只出現在別人的 neighbor，補一個空陣列以便演算法繼續
				$graph[$start] = [];
			}
			// 同理確保 $end 至少存在於圖的節點集合（即便沒有出邊）
			if (!isset($graph[$end]) && !array_key_exists($end, $graph)) {
				$graph[$end] = [];
			}

			// 蒐集所有節點（包含只出現在 neighbor 的點）
			$nodes = array_fill_keys(array_keys($graph), true);
			foreach ($graph as $u => $adj) {
				foreach ($adj as $v => $_) $nodes[$v] = true;
			}

			// 初始化距離與前驅
			$dist = [];
			$prev = [];
			foreach ($nodes as $node => $_) {
				$dist[$node] = INF;
				$prev[$node] = null;
			}
			$dist[$start] = 0;

			// 使用 SplPriorityQueue（注意它是最大堆，priority 取負數）
			$pq = new \SplPriorityQueue();
			$pq->setExtractFlags(\SplPriorityQueue::EXTR_DATA);
			$pq->insert($start, 0);

			while (!$pq->isEmpty()) {
				$u = $pq->extract();

				if ($u === $end) break; // 走到終點即可停止（Dijkstra 性質）

				if (!isset($graph[$u])) continue;
				foreach ($graph[$u] as $v => $w) {
					$alt = $dist[$u] + $w;
					if ($alt < $dist[$v]) {
						$dist[$v] = $alt;
						$prev[$v] = $u;
						$pq->insert($v, -$alt); // 負數做成最小距離優先
					}
				}
			}

			if (!is_finite($dist[$end])) {
				return [
					'found' => false,
					'distance' => null,
					'path' => []
				];
			}

			// 回溯路徑
			$path = [];
			for ($at = $end; $at !== null; $at = $prev[$at]) {
				$path[] = $at;
			}
			$path = array_reverse($path);

			return [
				'found' => true,
				'distance' => $dist[$end], // 總時間（與 Time 同單位）
				'path' => $path
			];
		}


		/* 獲取捷運站點資料 */
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
				

		/* 建立使用者資料 */
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