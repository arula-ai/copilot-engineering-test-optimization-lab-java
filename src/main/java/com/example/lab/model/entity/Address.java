package com.example.lab.model.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    public boolean isValid() {
        return street != null && street.length() >= 5 &&
               city != null && city.length() >= 2 &&
               state != null && state.length() >= 2 &&
               postalCode != null && postalCode.length() >= 5 &&
               country != null && country.length() >= 2;
    }
}
