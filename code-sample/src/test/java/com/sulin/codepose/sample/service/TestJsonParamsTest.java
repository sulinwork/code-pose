package com.sulin.codepose.sample.service;

import com.sulin.codepose.kit.json.Gsons;
import com.sulin.codepose.paramstest.annotation.JsonFileSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
public class TestJsonParamsTest {

    @ParameterizedTest
    @JsonFileSource(value = "test.json",multiMapping = true)
    public void test(List<String> ids, List<String> names) {

        log.info("{}", ids);
        log.info("{}", names);
    }
}
