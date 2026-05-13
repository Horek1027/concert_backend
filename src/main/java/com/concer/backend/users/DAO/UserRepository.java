package com.concer.backend.users.DAO;

import com.concer.backend.users.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users,Integer> {
    Users findByAccount(String account);


    @Modifying
    @Query("Update Users u set  u.nickname =:nickname , u.email=:email , " +
            "u.cellphone=:cellphone where u.account=:account")
    void updateUsersDetail( String account, String nickname, String email ,String cellphone);

    @Modifying
    @Query("UPDATE Users u SET u.password = :password WHERE u.account = :account")
    void updatePassword(String account, String password);
}
