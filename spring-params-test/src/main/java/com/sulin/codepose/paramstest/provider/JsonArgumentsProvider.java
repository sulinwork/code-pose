package com.sulin.codepose.paramstest.provider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.util.IOUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.sulin.codepose.kit.json.Gsons;
import com.sulin.codepose.paramstest.annotation.JsonFileSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class JsonArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        Method requiredTestMethod = extensionContext.getRequiredTestMethod();
        JsonFileSource annotation = requiredTestMethod.getAnnotation(JsonFileSource.class);
        try {
            String filePath = annotation.value();
            if (!filePath.startsWith("/")) {
                filePath = "/" + filePath;
            }
            InputStream resourceAsStream = extensionContext.getRequiredTestClass().getResourceAsStream(filePath);
            if (Objects.isNull(resourceAsStream)) {
                throw new IllegalArgumentException(String.format("json file:%s not exists", annotation.value()));
            }
            InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
            return annotation.multiMapping() ? parseMultiMappingArgument(extensionContext, inputStreamReader) : parseSingleArgument(extensionContext, inputStreamReader);

        } catch (Exception e) {
            log.error("json file parse error", e);
            return Stream.empty();
        }
    }

    private Stream<? extends Arguments> parseMultiMappingArgument(ExtensionContext extensionContext, InputStreamReader fileStreamReader) {
        //gson解析fileStreamReader成json tree
        JsonElement jsonElement = Gsons.GSON.fromJson(fileStreamReader, JsonElement.class);
        if (!jsonElement.isJsonObject()) {
            throw new IllegalArgumentException("json format must be object");
        }
        final JsonObject asJsonObject = jsonElement.getAsJsonObject();
        Parameter[] parameters = extensionContext.getRequiredTestMethod().getParameters();
        //返回
        return Stream.of(Arguments.of(Arrays.stream(parameters)
                .map(p -> {
                    String name = p.getName();
                    Type parameterizedType = p.getParameterizedType();
                    JsonElement element = asJsonObject.get(name);
                    if (Objects.isNull(element)) return null;
                    return Gsons.GSON.fromJson(element.toString(), parameterizedType);
                }).toArray()));

    }

    private Stream<? extends Arguments> parseSingleArgument(ExtensionContext extensionContext, InputStreamReader fileStreamReader) {
        Type genericParameterType = extensionContext.getRequiredTestMethod().getGenericParameterTypes()[0];
        return Stream.of(Gsons.GSON.fromJson(fileStreamReader, genericParameterType)).map(Arguments::of);
    }
}
