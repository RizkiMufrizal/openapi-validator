package com.axway.library.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class MessageValidation implements Serializable {

    private String key;
    private String message;
}