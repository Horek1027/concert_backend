Drop database  if exists concert;

CREATE database `concert` DEFAULT CHARACTER SET utf8mb4 ;

-- 使用者
create table if not exists concert.`users`
(
`user_id` int not null auto_increment comment '使用者編號' primary key,
`account` varchar(50) not null comment '使用者名稱',
`password` varchar(68) not null comment '密碼',
`nikcname` varchar(50) not null comment '暱稱',
`email` varchar(50) not null comment '信箱',
`cellphone` varchar(50) not null comment '電話',
`status` int not null comment '狀態'
) comment '使用者';

-- 活動
create table if not exists concert.`events`
(
`events_id` int not null auto_increment comment '活動編號' primary key,
`userid` int not null comment '會員編號',
`events_name` varchar(100) not null comment '活動名稱',
`events_details` varchar(300) not null comment '活動介紹',
`events_location` varchar(100) not null comment '地點',
`events_organizer` varchar(50) not null comment '主辦單位',
`event_date` varchar(50) not null comment '活動日期',
`shelf_time` datetime not null default CURRENT_TIMESTAMP comment '上架日期',
`offsale_time` datetime not null  comment '下架日期',
`image1` varchar(50) comment '封面圖片'
) comment '活動';

create table if not exists concert.`area`
(
`area_id` int not null auto_increment comment '區域編號' primary key,
`events_id` int not null  comment'活動編號',
`area_name` varchar(30) not null comment '區域',
`area_price` int not null comment '價格', 
`qty`  int not null comment '庫存',
constraint fk_events_id foreign key(`events_id`) references concert.`events`(`events_id`)
) comment '區域';



-- 訂單
create table if not exists concert.`orders`
(
`order_id` int not null auto_increment comment '訂單編號' primary key,
`users_id` int not null comment '會員編號',
`events_id` int not null comment '活動編號',
`order_area` varchar(30) not null comment '座位區域',
`order_qty` int not null comment '訂購數量',
`order_price` int not null comment '活動價格',
`order_date` datetime not null default CURRENT_TIMESTAMP comment '訂購日期',
constraint fk_product_order_users foreign key (`users_id`) references concert.users(`user_id`),
constraint fk_product_order_events foreign key (`events_id`) references concert.events(`events_id`)
) comment '訂單';

