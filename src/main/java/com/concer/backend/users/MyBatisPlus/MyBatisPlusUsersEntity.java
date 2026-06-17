package com.concer.backend.users.MyBatisPlus;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class MyBatisPlusUsersEntity {
    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;
    private String account;
    private String password;
    private String nickname;
    private String email;
    @TableField("cellphone")
    private String cellphone;
    private Integer status;
}
