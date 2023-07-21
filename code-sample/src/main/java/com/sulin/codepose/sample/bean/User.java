package com.sulin.codepose.sample.bean;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class User {
    private Long userId;
    private String name;
}
