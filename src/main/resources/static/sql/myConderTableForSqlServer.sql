USE master
/*使用master在檢查是否存在 concert 資料庫，以及刪除和創建資料庫*/
GO -- go 是SQL Server的工具代表分割邏輯以及順序
/*當 SQL Server 管理工具遇到 GO 關鍵字時，
它會將前面的所有 SQL 代碼發送給伺服器以執行，
 然後清除這些語句，並等待下一個輸入。*/

--DROP DATABASE  if exists concert; -- 就是drop資料庫
-- GO

CREATE DATABASE concert;
GO


use concert
 
-- 使用者
create table users
(
user_id int identity(1,1) primary key,
account varchar(50) not null ,
password varchar(68) not null ,
nickname varchar(50) not null ,
email varchar(50) not null ,
cellphone varchar(50) not null ,
status int not null 
) ;

-- 活動
create table events
(
events_id int identity(1,1) primary key,
user_id int not null ,
events_name varchar(100) not null ,
events_details varchar(3000) not null ,
events_location varchar(100) not null,
events_organizer varchar(50) not null ,
event_date varchar(50) not null ,
shelf_time datetime not null default CURRENT_TIMESTAMP ,
offsale_time datetime not null ,
image1 varchar(50) 
);

create table area
(
area_id int identity(1,1) primary key,
events_id int not null  ,
area_name varchar(30) not null ,
area_price int not null , 
qty int not null ,
constraint fk_events_id foreign key(events_id) references events(events_id)
) ;


-- 訂單

create table orders
(
order_id int identity(1,1) primary key,
users_id int not null ,
events_id int not null ,
order_area varchar(30) not null ,
order_qty int not null ,
order_price int not null ,
order_date datetime not null default CURRENT_TIMESTAMP ,
order_status int default 0,
constraint fk_product_order_users foreign key (users_id) references users(user_id),
constraint fk_product_order_events foreign key (events_id) references events(events_id)
);

