package com.frank.mybatis.chapter01.mapper;

import com.frank.mybatis.annotations.*;
import com.frank.mybatis.fixture.TUser;

import java.util.List;

public interface UserMapper {
    @Insert("insert into t_user(id,user_name,age) values(#{id},#{name},#{age})")
    int insert(@Param("id") Long id, @Param("name") String name, @Param("age") Integer age);

    @Select("select id,user_name,age from t_user where id=#{id}")
    TUser findById(@Param("id") Long id);

    @Select("select id,user_name,age from t_user where age >= #{minAge} order by id")
    List<TUser> findByMinimumAge(@Param("minAge") Integer minAge);

    @Update("update t_user set user_name=#{name} where id=#{id}")
    int rename(@Param("id") Long id, @Param("name") String name);

    @Delete("delete from t_user where id=#{arg0}")
    int deleteById(Long id);

}
