package com.sulin.codepose.paramstest.provider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.util.IOUtils;
import com.sulin.codepose.paramstest.annotation.JsonFileSource;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

public class JsonArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        Method requiredTestMethod = extensionContext.getRequiredTestMethod();
        JsonFileSource annotation = requiredTestMethod.getAnnotation(JsonFileSource.class);
        Type type = requiredTestMethod.getGenericParameterTypes()[0];
        if (type instanceof ParameterizedType) {
            String json = readJsonFile(extensionContext, annotation.value());
            ParameterizedType parameterType = (ParameterizedType) type;
            if (parameterType.getRawType().getTypeName().equals(String.class.getTypeName())) {
                return Stream.of(json).map(Arguments::of);
            }
            if (parameterType.getRawType().getTypeName().equals(List.class.getTypeName())) {
                Type actualTypeArgument = parameterType.getActualTypeArguments()[0];
                return Stream.of(JSON.parseArray(json,actualTypeArgument)).map(Arguments::of);
            }
            return Stream.of(JSON.parseObject(json, parameterType)).map(Arguments::of);
        }
        return Stream.empty();
    }


    private String readJsonFile(ExtensionContext context, String path) throws IOException {
        InputStream is = context.getRequiredTestClass().getResourceAsStream(path);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        String data = null;
        while ((data = br.readLine()) != null) {
            sb.append(data);
        }

        br.close();
        isr.close();
        is.close();
        return sb.toString();
    }

}
