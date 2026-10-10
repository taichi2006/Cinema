package com.cinema.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectMapperConfig {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // Không bao gồm các trường null khi parse object sang json
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Bỏ qua các thuộc tính không có trong class khi parse json sang object
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Hỗ trợ parse/format các kiểu thời gian mới của Java 8 (LocalDateTime, LocalDate...)
        mapper.registerModule(new JavaTimeModule());
        // Trả về date format chuẩn ISO-8601 thay vì mảng timestamp
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public static ObjectMapper getInstance() {
        return mapper;
    }
}
