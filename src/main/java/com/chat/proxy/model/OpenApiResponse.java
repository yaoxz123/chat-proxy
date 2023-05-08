package com.chat.proxy.model;

import lombok.Data;

import java.util.List;

@Data
public class OpenApiResponse {
    private List<Choices> choices;
}
