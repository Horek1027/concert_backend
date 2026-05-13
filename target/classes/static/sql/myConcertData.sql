INSERT INTO `users`  
VALUES (
    1,
    'Admin',
    '$2a$10$sI80I.KgixqsYZ5vnH7GSO1rAw20XPXqJSGWLMfAfcGmcsUipFH/6',-- admin123
    '大大管理者',
    'Admin@gmail.com',
    '0900-000-000',
    1
), (
    2,
    'user',
    '$2a$10$q8ynT4NeCioUEA4yY0n9HeZmG3ednzFNXQ4ub/deDl8.L/up1vqCu',-- abc123
    '小小管理者',
    'user@gmail.com',
    '0900-000-001',
    0
);

-- 活動
INSERT INTO `events` 
VALUES (
    1,
    1,
    '回歸最初的樣子：生日音樂會｜HAOR許書豪',
    '一把木吉他 穿越人生不同的階段，真摯的能量  歷經攀高或失足 都不曾消減',
    '女巫店（台北市大安區新生南路三段56巷7號1樓）',
    '這方娛樂',
    '2024/3/15（日）19:30',
    '2023-12-09 12:20:52',
	'2024-03-14 12:20:52',
    'test1.png'
),
 (
    2,
    1,
    'RADWIMPS WORLD TOUR 2024 ',
    '日本人氣搖滾樂團RADWIMPS，2023年成功的完成世界巡迴演唱會，並且創下場場完售的佳績紀錄。現在正式宣布，將於2024春季展開「RADWIMPS WORLD TOUR 2024 “The way you yawn, and the outcry of Peace”」
    亞洲巡迴演出',
    '台北流行音樂中心 ( 台北市南港區市民大道8段99號 )',
    '大國文化/好玩國際文化',
    '2024/5/15(三)20:00',
	'2024-02-04 14:07:28',
	'2024-05-10 14:07:28',
    'test2.png'
);

-- 區域
INSERT INTO `area`
values
(1,1,'A區',1500,50),
(2,2,'1F搖滾區',5500,50),(3,2,'1F座位區',4800,70);

-- 訂單
Insert into `orders` VALUES
(1,2,1,'A區',2,1500,'2024-03-14 12:20:52'),
(2,2,2,'1F搖滾區',2,5500,'2024-03-10 12:20:52'),
(3,2,2,'1F座位區',3,4800,'2024-04-10 12:20:52');
