package com.sulin.codepose.paramstest;

import com.sulin.codepose.paramstest.annotation.JsonFileSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class ATest {

    class Test {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    //    @Test
    @ParameterizedTest
    @JsonFileSource("/test.json")
    public void test(List<Test> tests) {
        System.out.println(tests);

    }
}