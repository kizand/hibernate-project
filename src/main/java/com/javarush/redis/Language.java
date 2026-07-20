package com.javarush.redis;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class Language {

    private String language;

    private Boolean isOfficial;

    private BigDecimal percentage;

}
