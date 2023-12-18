package com.axway.library.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HttpResponse implements Serializable {
    private Integer httpCode;
    private String httpMessage;
    private String body;
}
