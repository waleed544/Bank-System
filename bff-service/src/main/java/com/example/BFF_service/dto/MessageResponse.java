package com.example.BFF_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Builder
@Getter
@Setter
public class MessageResponse {

    private String message;

}
