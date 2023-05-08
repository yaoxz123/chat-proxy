package com.chat.proxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OpenApiRequest {
    @JsonProperty(value  = "model")
    private String model;
    @JsonProperty(value  = "messages")
    private List<Chat> messages;


}
