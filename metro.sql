-- MySQL dump 10.13  Distrib 8.0.38, for Win64 (x86_64)
--
-- Host: 140.131.115.94    Database: metro
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `line`
--

DROP TABLE IF EXISTS `line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `line` (
  `LineCode` varchar(2) NOT NULL COMMENT '各線代表號',
  `NameE` varchar(50) NOT NULL COMMENT '各線(英)',
  `NameC` varchar(20) NOT NULL COMMENT '各線(中)',
  PRIMARY KEY (`LineCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='捷運各線資料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `line`
--

LOCK TABLES `line` WRITE;
/*!40000 ALTER TABLE `line` DISABLE KEYS */;
INSERT INTO `line` VALUES ('BL','Bannan Line','板南線'),('BR','Wenhu Line','文湖線'),('G','Songshan-Xindian Line','松山新店線'),('O','Zhonghe-Xinlu Line','中和新蘆線'),('R','Tamsui–Xinyi Line','淡水信義線'),('Y','Circular Line','環狀線');
/*!40000 ALTER TABLE `line` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `station`
--

DROP TABLE IF EXISTS `station`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `station` (
  `SNo` int NOT NULL AUTO_INCREMENT COMMENT '站點流水號',
  `StationCode` varchar(4) NOT NULL COMMENT '站點代表號',
  `NameE` varchar(50) NOT NULL COMMENT '站點(英)',
  `NameC` varchar(20) NOT NULL COMMENT '站點(中)',
  `LineCode` varchar(2) NOT NULL COMMENT '各線代表號',
  PRIMARY KEY (`SNo`),
  KEY `FK_station_LineCode_idx` (`LineCode`),
  CONSTRAINT `FK_station_LineCode` FOREIGN KEY (`LineCode`) REFERENCES `line` (`LineCode`)
) ENGINE=InnoDB AUTO_INCREMENT=71 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='捷運站點資料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `station`
--

LOCK TABLES `station` WRITE;
/*!40000 ALTER TABLE `station` DISABLE KEYS */;
INSERT INTO `station` VALUES (1,'BR01','Taipei Zoo','動物園','BR'),(2,'BR02','Muzha','木柵','BR'),(3,'BR03','Wanfang Community','萬芳社區','BR'),(4,'BR04','Wanfang Hospital','萬芳醫院','BR'),(5,'BR05','Xinhai','辛亥','BR'),(6,'BR06','Linguang','麟光','BR'),(7,'BR07','Liuzhangli','六張犁','BR'),(8,'BR08','Technology Building','科技大樓','BR'),(9,'BR09','Daan','大安','BR'),(10,'BR10','Zhongxiao Fuxing','忠孝復興','BR'),(11,'BR11','Nanjing Fuxing','南京復興','BR'),(12,'BR12','Zhongshan Junior High School','中山國中','BR'),(13,'BR13','Songshan Airport','松山機場','BR'),(14,'BR14','Dazhi','大直','BR'),(15,'BR15','Jiannan Road','劍南路','BR'),(16,'BR16','Xihu','西湖','BR'),(17,'BR17','Gangqian','港墘','BR'),(18,'BR18','Wende','文德','BR'),(19,'BR19','Neihu','內湖','BR'),(20,'BR20','Dahu Park','大湖公園','BR'),(21,'BR21','Huzhou','葫洲','BR'),(22,'BR22','Donghu','東湖','BR'),(23,'BR23','Nangang Software Park','南港軟體園區','BR'),(24,'BR24','Taipei Nangang Exhibition Center','南港展覽館','BR'),(25,'R02','Xiangshan','象山','R'),(26,'R03','Taipei 101/World Trade Center','台北101／世貿','R'),(27,'R03','Taipei 101/World Trade Center','台北101／世貿','R'),(28,'R04','Xinyi Anhe','信義安和','R'),(29,'R05','Daan','大安','R'),(30,'R06','Daan Park','大安森林公園','R'),(31,'R07','Dongmen','東門','R'),(32,'R08','Chiang Kai-Shek Memorial Hall','中正紀念堂','R'),(33,'R09','National Taiwan University Hospital','台大醫院','R'),(34,'R10','Taipei Main Station','台北車站','R'),(35,'R11','Zhongshan','中山','R'),(36,'R12','Shuanglian','雙連','R'),(37,'R13','Minquan West Road','民權西路','R'),(38,'R14','Yuanshan','圓山','R'),(39,'R15','Jiantan','劍潭','R'),(40,'R16','Shilin','士林','R'),(41,'R17','Zhishan','芝山','R'),(42,'R18','Mingde','明德','R'),(43,'R19','Shipai','石牌','R'),(44,'R20','Qilian','唭哩岸','R'),(45,'R22','Beitou','北投','R'),(46,'R23','Fuxinggang','復興崗','R'),(47,'R24','Zhongyi','忠義','R'),(48,'R25','Guandu','關渡','R'),(49,'R26','Zhuwei','竹圍','R'),(50,'R27','Hongshulin','紅樹林','R'),(51,'R28','Tamsui','淡水','R'),(52,'G01','Xindian','新店','G'),(53,'G02','Xindian District Office','新店區公所','G'),(54,'G03','Qizhang','七張','G'),(55,'G04','Dapinglin','大坪林','G'),(56,'G05','Jingmei','景美','G'),(57,'G06','Wanlong','萬隆','G'),(58,'G07','Gongguan','公館','G'),(59,'G08','Taipower Building','台電大樓','G'),(60,'G09','Guting','古亭','G'),(61,'G10','Chiang Kai-Shek Memorial Hall','中正紀念堂','G'),(62,'G11','Xiaonanmen','小南門','G'),(63,'G12','Ximen','西門','G'),(64,'G13','Beimen','北門','G'),(65,'G14','Zhongshan','中山','G'),(66,'G15','Songjiang Nanjing','松江南京','G'),(67,'G16','Nanjing Fuxing','南京復興','G'),(68,'G17','Taipei Arena','台北小巨蛋','G'),(69,'G18','Nanjing Sanmin','南京三民','G'),(70,'G19','Songshan','松山','G');
/*!40000 ALTER TABLE `station` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `station_place`
--

DROP TABLE IF EXISTS `station_place`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `station_place` (
  `SPNo` int NOT NULL AUTO_INCREMENT COMMENT '地點流水號',
  `NameE` varchar(50) NOT NULL COMMENT '地點(英)',
  `NameC` varchar(20) NOT NULL COMMENT '地點(中)',
  `AddressE` varchar(50) NOT NULL COMMENT '地址(英)',
  `AddressC` varchar(200) NOT NULL COMMENT '地址(中)',
  `SNo` int NOT NULL COMMENT '站點流水號',
  `TNo` smallint NOT NULL COMMENT '分類標籤流水號',
  `CreateTime` datetime NOT NULL COMMENT '建立時間',
  `UpdateTime` datetime NOT NULL COMMENT '更新時間',
  PRIMARY KEY (`SPNo`),
  KEY `FK_station_place_SNo_idx` (`SNo`),
  KEY `FK_station_place_TNo_idx` (`TNo`),
  CONSTRAINT `FK_station_place_SNo` FOREIGN KEY (`SNo`) REFERENCES `station` (`SNo`),
  CONSTRAINT `FK_station_place_TNo` FOREIGN KEY (`TNo`) REFERENCES `station_place_tag` (`TNo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站點附近景點資料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `station_place`
--

LOCK TABLES `station_place` WRITE;
/*!40000 ALTER TABLE `station_place` DISABLE KEYS */;
/*!40000 ALTER TABLE `station_place` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `station_place_tag`
--

DROP TABLE IF EXISTS `station_place_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `station_place_tag` (
  `TNo` smallint NOT NULL COMMENT '分類標籤流水號',
  `NameE` varchar(50) NOT NULL COMMENT '地點分類',
  `NameC` varchar(20) NOT NULL,
  PRIMARY KEY (`TNo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地點分類標籤資料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `station_place_tag`
--

LOCK TABLES `station_place_tag` WRITE;
/*!40000 ALTER TABLE `station_place_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `station_place_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_like`
--

DROP TABLE IF EXISTS `user_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_like` (
  `ULNo` int NOT NULL AUTO_INCREMENT COMMENT '使用者收藏紀錄流水號',
  `UserNo` int NOT NULL COMMENT '使用者流水號',
  `SPNo` int NOT NULL COMMENT '地點流水號',
  `CreateTime` datetime NOT NULL COMMENT '建立時間',
  `UpdateTime` datetime NOT NULL COMMENT '更新時間',
  PRIMARY KEY (`ULNo`),
  KEY `FK_user_like_UserNo_idx` (`UserNo`),
  KEY `FK_user_like_SPNo_idx` (`SPNo`),
  CONSTRAINT `FK_user_like_SPNo` FOREIGN KEY (`SPNo`) REFERENCES `station_place` (`SPNo`),
  CONSTRAINT `FK_user_like_UserNo` FOREIGN KEY (`UserNo`) REFERENCES `user_login` (`UserNo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='使用者收藏紀錄資料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_like`
--

LOCK TABLES `user_like` WRITE;
/*!40000 ALTER TABLE `user_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_login`
--

DROP TABLE IF EXISTS `user_login`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_login` (
  `UserNo` int NOT NULL AUTO_INCREMENT COMMENT '使用者流水號',
  `Gmail` varchar(64) NOT NULL COMMENT '電子信箱',
  `IsStop` char(1) NOT NULL COMMENT '帳號是否停用,Y/N',
  `CreateTime` datetime NOT NULL COMMENT '建立時間',
  PRIMARY KEY (`UserNo`),
  UNIQUE KEY `email_UNIQUE` (`Gmail`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='使用者登入資料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_login`
--

LOCK TABLES `user_login` WRITE;
/*!40000 ALTER TABLE `user_login` DISABLE KEYS */;
INSERT INTO `user_login` VALUES (1,'11336010@ntub.edu.tw','N','2025-05-04 17:17:40');
/*!40000 ALTER TABLE `user_login` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_profile`
--

DROP TABLE IF EXISTS `user_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profile` (
  `UserNo` int NOT NULL COMMENT '使用者流水號',
  `UserName` varchar(20) NOT NULL COMMENT '名稱',
  `UserImageUrl` varchar(50) NOT NULL COMMENT '照片路徑',
  `UpdateTime` datetime NOT NULL COMMENT '更新時間',
  PRIMARY KEY (`UserNo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='使用者基本資料資料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_profile`
--

LOCK TABLES `user_profile` WRITE;
/*!40000 ALTER TABLE `user_profile` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_profile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `v_line_station`
--

DROP TABLE IF EXISTS `v_line_station`;
/*!50001 DROP VIEW IF EXISTS `v_line_station`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_line_station` AS SELECT 
 1 AS `StationCode`,
 1 AS `Station`,
 1 AS `Line`,
 1 AS `LineCode`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `v_line_station`
--

/*!50001 DROP VIEW IF EXISTS `v_line_station`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`metro`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_line_station` AS select `s`.`StationCode` AS `StationCode`,`s`.`NameE` AS `Station`,`l`.`NameE` AS `Line`,`l`.`LineCode` AS `LineCode` from (`station` `s` join `line` `l` on((`s`.`LineCode` = `l`.`LineCode`))) order by `s`.`SNo` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-05-20  2:34:55
