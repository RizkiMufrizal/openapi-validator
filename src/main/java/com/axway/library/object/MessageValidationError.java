package com.axway.library.object;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class MessageValidationError implements Serializable {
    private List<MessageValidation> messageValidationRequired;
    private Boolean isErrorRequired;

    private List<MessageValidation> messageValidationFormatError;
    private Boolean isErrorFormat;
}