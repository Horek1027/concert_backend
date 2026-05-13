package com.concer.backend.util;

import com.concer.backend.users.Entity.Users;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Lock {


    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    //把user資料用 BCrypt 方法加密，作為token回傳給前端
    public static String encryptUsers(Users users){
        try{
            String usersToString =users.toString();
            String token = encoder.encode(usersToString);
//            System.out.println("我是token:"  + token);
            return token;

        }catch (Exception e){
            e.printStackTrace();
            return "出現錯誤";
        }
    }
    //下方比對Users是否相同
    public static boolean checkUsers(Users orginalUser, String token ){
        //直接把users 轉string 去跟 BCrypt後的token去比較
        String stringInput = orginalUser.toString();

        return encoder.matches(stringInput,token);
        //BCrypt 提供的 matches 方法来比较密码是否匹配
    }



    public static String encryptPassword(String pw){
        try{
            return encoder.encode(pw);// 使用 BCryptPasswordEncoder 加密
        }catch(Exception e){
            e.printStackTrace();;
            return "出現錯誤";
        }
    }
    //下方比對密碼是否相同
    public static boolean checkPasssword(String inputPassword , String encryptedPassword){

//        System.out.println("input密碼:" + inputPassword);
//        System.out.println("DB密碼:" + encryptedPassword);
        return encoder.matches(inputPassword,encryptedPassword);
        //BCrypt 提供的 matches 方法来比较密码是否匹配
    }

}
