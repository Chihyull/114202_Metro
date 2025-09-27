<?php

	class DbOperations{
		
		private $con;

		function __construct(){

			require_once dirname(__FILE__).'/DbConnect.php';

			$db = new DbConnect();

			$this->con = $db->connect();
		}

		public function upsertStationPlace($seno, $placeId) {
			return $this->upsertStationPlaceFull($seno, $placeId, null, null);
		}

		public function upsertStationPlaceFull($seno, $placeId, $photoRef, $nameE) {
			$sql = "
				INSERT INTO metro.station_place (SENo, PlaceID, PhotoRef, NameE, UpdateTime)
				VALUES (?, ?, ?, ?, NOW())
				ON DUPLICATE KEY UPDATE
					PhotoRef = VALUES(PhotoRef),
					NameE    = VALUES(NameE),
					UpdateTime = NOW()
			";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return false;
			$stmt->bind_param('isss', $seno, $placeId, $photoRef, $nameE);
			return $stmt->execute();
		}

		/** 依 SENo 遞增分批抓出口（原樣保留） */
		public function getExitBatch($startSENo = 0, $limit = 200) {
			$sql = "SELECT SENo, StationCode, `Exit`, Longitude, Latitude
					FROM metro.station_exit_locat
					WHERE SENo > ?
					ORDER BY SENo ASC
					LIMIT ?";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return [];
			$stmt->bind_param("ii", $startSENo, $limit);
			$stmt->execute();
			$res = $stmt->get_result();

			$rows = [];
			while ($row = $res->fetch_assoc()) {
				$rows[] = [
					'SENo'        => (int)$row['SENo'],
					'StationCode' => $row['StationCode'],
					'Exit'        => $row['Exit'],
					'Longitude'   => (float)$row['Longitude'],
					'Latitude'    => (float)$row['Latitude']
				];
			}
			return $rows;
		}


		/* 檢查使用者是否已有同名資料夾；有的話回傳 ULNo，沒有則回 0 */
		public function getUserLikeByName($userNo, $fileName) {
			$sql = "SELECT ULNo FROM metro.user_like WHERE UserNo = ? AND FileName = ? LIMIT 1";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return 0;
			$stmt->bind_param("is", $userNo, $fileName);
			$stmt->execute();
			$res = $stmt->get_result();
			if ($row = $res->fetch_assoc()) {
				return (int)$row['ULNo'];
			}
			return 0;
		}

		/* ===========================收藏功能開始=========================== */
		/* 新增我的最愛資料夾 */
		public function createUserLike($userNo, $fileName) {
			// 先檢查同名
			$exists = $this->getUserLikeByName($userNo, $fileName);
			if ($exists > 0) {
				return ['status' => 'exists', 'ULNo' => $exists];
			}

			$sql = "INSERT INTO metro.user_like (FileName, UserNo, CreateTime, UpdateTime)
					VALUES (?, ?, NOW(), NOW())";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return ['status' => 'error', 'ULNo' => 0];

			$stmt->bind_param("si", $fileName, $userNo);
			if (!$stmt->execute()) {
				return ['status' => 'error', 'ULNo' => 0];
			}
			return ['status' => 'created', 'ULNo' => (int)$stmt->insert_id];
		}

		public function upsertFavorite($ulNo, $spNo, $isFavorite) {
			$sql = "
				INSERT INTO metro.user_like_place (ULNo, SPNo, IsFavorite, CreateTime, UpdateTime)
				VALUES (?, ?, ?, NOW(), NOW())
				ON DUPLICATE KEY UPDATE
					IsFavorite = VALUES(IsFavorite),
					UpdateTime = NOW()
			";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return false;
			$isFavorite = (int)($isFavorite ? 1 : 0);
			$stmt->bind_param("iii", $ulNo, $spNo, $isFavorite);
			return $stmt->execute();
		}
		/** 設定收藏為 1（加入收藏；若不存在則建立） */
		public function setFavorite($ulNo, $spNo) {
			return $this->upsertFavorite($ulNo, $spNo, 1);
		}
		/** 設定收藏為 0（取消收藏；若不存在則補一筆 0 以保持一致語意） */
		public function unsetFavorite($ulNo, $spNo) {
			$sql = "UPDATE metro.user_like_place
			        SET IsFavorite = 0, UpdateTime = NOW()
			        WHERE ULNo = ? AND SPNo = ?";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return false;
			$stmt->bind_param("ii", $ulNo, $spNo);
			$stmt->execute();

			// 若沒有任何列被更新，補插一筆 IsFavorite=0（可選；保留紀錄一致性）
			if ($stmt->affected_rows === 0) {
				return $this->upsertFavorite($ulNo, $spNo, 0);
			}
			return true;
		}
		/** 切換收藏（按一下就反轉）；若不存在，視為加入收藏（1） */
		public function toggleFavorite($ulNo, $spNo) {
			$sql = "UPDATE metro.user_like_place
			        SET IsFavorite = 1 - IsFavorite, UpdateTime = NOW()
			        WHERE ULNo = ? AND SPNo = ?";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return false;
			$stmt->bind_param("ii", $ulNo, $spNo);
			$stmt->execute();

			// 若無紀錄則建立為收藏=1
			if ($stmt->affected_rows === 0) {
				return $this->upsertFavorite($ulNo, $spNo, 1);
			}
			return true;
		}
		/** 刪除整個資料夾時，把該資料夾的所有收藏都設為 0（不刪紀錄，保留歷史） */
		public function clearFolderFavorites($ulNo) {
			$sql = "UPDATE metro.user_like_place
			        SET IsFavorite = 0, UpdateTime = NOW()
			        WHERE ULNo = ?";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return false;
			$stmt->bind_param("i", $ulNo);
			return $stmt->execute();
		}

		/** 查詢某資料夾目前 IsFavorite=1 的地點清單（含地點資訊） */
		public function getFavoritesInFolder($ulNo) {
			$sql = "
				SELECT sp.SPNo, sp.SENo, sp.PlaceID, sp.PhotoRef, sp.NameE
				FROM metro.user_like_place ulp
				JOIN metro.station_place sp ON sp.SPNo = ulp.SPNo
				WHERE ulp.ULNo = ? AND ulp.IsFavorite = 1
				ORDER BY sp.NameE
			";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return [];
			$stmt->bind_param("i", $ulNo);
			$stmt->execute();
			$res = $stmt->get_result();

			$items = [];
			while ($row = $res->fetch_assoc()) {
				$items[] = [
					'SPNo'     => (int)$row['SPNo'],
					'SENo'     => (int)$row['SENo'],
					'PlaceID'  => $row['PlaceID'],
					'PhotoRef' => $row['PhotoRef'],
					'NameE'    => $row['NameE']
				];
			}
			return $items;
		}

		/* 刪除我的最愛資料夾（先清 IsFavorite=0→刪 mapping→刪資料夾本身） */
		public function deleteUserLike($userNo, $ulNo) {
			// 確認資料夾歸屬
			$chk = $this->con->prepare("SELECT 1 FROM metro.user_like WHERE ULNo=? AND UserNo=? LIMIT 1");
			if (!$chk) return false;
			$chk->bind_param("ii", $ulNo, $userNo);
			$chk->execute();
			$res = $chk->get_result();
			if ($res->num_rows === 0) return false;

			$this->con->begin_transaction();
			try {
				// 1) 全部標 0（符合你的規則）
				$stmt = $this->con->prepare("UPDATE metro.user_like_place SET IsFavorite=0, UpdateTime=NOW() WHERE ULNo=?");
				if (!$stmt) throw new Exception('prep fail: update favorites 0');
				$stmt->bind_param("i", $ulNo);
				if (!$stmt->execute()) throw new Exception('exec fail: update favorites 0');

				// 2) 真的移除 mapping（避免外鍵擋刪）
				$stmt = $this->con->prepare("DELETE FROM metro.user_like_place WHERE ULNo=?");
				if (!$stmt) throw new Exception('prep fail: delete mapping');
				$stmt->bind_param("i", $ulNo);
				if (!$stmt->execute()) throw new Exception('exec fail: delete mapping');

				// 3) 刪資料夾
				$stmt = $this->con->prepare("DELETE FROM metro.user_like WHERE ULNo=? AND UserNo=?");
				if (!$stmt) throw new Exception('prep fail: delete folder');
				$stmt->bind_param("ii", $ulNo, $userNo);
				if (!$stmt->execute()) throw new Exception('exec fail: delete folder');

				$this->con->commit();
				return true;
			} catch (\Throwable $e) {
				$this->con->rollback();
				return false;
			}
		}

		/*（可選）用名稱刪除：先找 ULNo 再呼叫上面 */
		public function deleteUserLikeByName($userNo, $fileName) {
			$ulNo = $this->getUserLikeByName($userNo, $fileName); // 你既有的方法
			if ($ulNo <= 0) return false;
			return $this->deleteUserLike($userNo, $ulNo);
		}

		/** 依使用者編號，取得我的最愛資料夾清單 */
		public function getUserLikes($userNo, $limit = 100, $offset = 0) {
			$sql = "
				SELECT ULNo, FileName, UserNo, CreateTime, UpdateTime
				FROM metro.user_like
				WHERE UserNo = ?
				ORDER BY UpdateTime DESC, ULNo DESC
				LIMIT ? OFFSET ?
			";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return [];

			// LIMIT / OFFSET 一律用整數
			$limit  = max(0, (int)$limit);
			$offset = max(0, (int)$offset);

			$stmt->bind_param("iii", $userNo, $limit, $offset);
			$stmt->execute();
			$res = $stmt->get_result();

			$rows = [];
			while ($row = $res->fetch_assoc()) {
				$rows[] = [
					'ULNo'       => (int)$row['ULNo'],
					'FileName'   => $row['FileName'],
					'UserNo'     => (int)$row['UserNo'],
					'CreateTime' => $row['CreateTime'],
					'UpdateTime' => $row['UpdateTime'],
				];
			}
			return $rows;
		}

		/** 依使用者編號，取得我的最愛資料夾總數（判斷 empty 用） */
		public function countUserLikes($userNo) {
			$sql = "SELECT COUNT(*) AS cnt FROM metro.user_like WHERE UserNo = ?";
			$stmt = $this->con->prepare($sql);
			if (!$stmt) return 0;
			$stmt->bind_param("i", $userNo);
			$stmt->execute();
			$res = $stmt->get_result();
			if ($row = $res->fetch_assoc()) {
				return (int)$row['cnt'];
			}
			return 0;
		}
		/* ===========================收藏功能結束=========================== */
		
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
				$sql = "
					SELECT StationCode, StationName, `Exit` AS ExitCode, photo, PlaceName
					FROM metro.v_station_exit_place
					WHERE StationCode = ?
					ORDER BY `Exit`, PlaceName
				";
				$stmt = $this->con->prepare($sql);
				if (!$stmt) return [];
				$stmt->bind_param("s", $stationCode);
			} else {
				$sql = "
					SELECT StationCode, StationName, `Exit` AS ExitCode, photo, PlaceName
					FROM metro.v_station_exit_place
					ORDER BY StationCode, `Exit`, PlaceName
				";
				$stmt = $this->con->prepare($sql);
				if (!$stmt) return [];
			}

			$stmt->execute();
			$result = $stmt->get_result();

			// 以 (StationCode, ExitCode) 分組，回傳 Places 陣列（PlaceName + photo）
			$grouped = []; // key: StationCode|ExitCode
			while ($row = $result->fetch_assoc()) {
				$key = $row['StationCode'] . '|' . $row['ExitCode'];
				if (!isset($grouped[$key])) {
					$grouped[$key] = [
						'StationCode'  => $row['StationCode'],
						'StationName'  => $row['StationName'], // 你要的站名欄位
						'ExitCode'     => $row['ExitCode'],
						'Places'       => []
					];
				}
				// 一筆地點
				$placeName = trim((string)$row['PlaceName']);
				$photo     = isset($row['photo']) ? trim((string)$row['photo']) : null;

				// 跳過全空的地點
				if ($placeName === '' && ($photo === null || $photo === '')) {
					continue;
				}

				$grouped[$key]['Places'][] = [
					'PlaceName' => $placeName,
					'photo'     => $photo
				];
			}

			// 輸出成陣列
			return array_values($grouped);
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