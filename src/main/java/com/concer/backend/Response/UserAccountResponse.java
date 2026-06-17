package com.concer.backend.Response;

import com.concer.backend.users.Entity.Users;
import com.concer.backend.users.MyBatisPlus.MyBatisPlusUsersEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountResponse {
    private MyBatisPlusUsersEntity users;
}
