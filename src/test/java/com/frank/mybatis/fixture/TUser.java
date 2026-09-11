package com.frank.mybatis.fixture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TUser {
    private Long id;
    private String userName;
    private Integer age;
}
