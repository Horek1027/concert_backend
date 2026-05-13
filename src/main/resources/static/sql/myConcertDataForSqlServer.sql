Use concert;

SET IDENTITY_INSERT users ON;

INSERT INTO users (user_id, account, password, nickname, email, cellphone, status)
VALUES (
    1,
    'admin',
    '$2a$10$I97cLg2RkbhA45AKagy/x.c1aclDNR/S90OGJdIXtmdB1CaBmyEOW', -- admin123
    '大大管理者',
    'Admin@gmail.com',
    '0900-000-000',
    1
), (
   2,
    'user',
    '$2a$10$OehhpO8qN4GSVSwb7TGOau0Yy8Nlrnb5s2C6OKeN0J.cFNvKjvIzK', -- user123
    '小小管理者',
    'user@gmail.com',
    '0900-000-001',
    0
);

SET IDENTITY_INSERT users OFF;


-- 活動
SET IDENTITY_INSERT events ON;

INSERT INTO events(events_id, user_id, events_name, events_details, events_location, events_organizer, event_date, shelf_time, offsale_time, image1)
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
SET IDENTITY_INSERT events OFF;

-- 區域
SET IDENTITY_INSERT area ON;

INSERT INTO area(area_id,events_id,area_name ,area_price,qty)
values
(1,1,'A區',1500,50),
(2,2,'1F搖滾區',5500,50),(3,2,'1F座位區',4800,70);
SET IDENTITY_INSERT area OFF;

-- 訂單
SET IDENTITY_INSERT orders ON;
Insert into orders (order_id,users_id,events_id,order_area,order_qty,order_price,order_date)
VALUES
(1,2,1,'A區',2,1500,'2024-03-14 12:20:52'),
(2,2,2,'1F搖滾區',2,5500,'2024-03-10 12:20:52'),
(3,2,2,'1F座位區',3,4800,'2024-04-10 12:20:52');
SET IDENTITY_INSERT orders OFF;