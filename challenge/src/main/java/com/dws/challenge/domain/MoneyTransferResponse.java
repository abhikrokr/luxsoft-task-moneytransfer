package com.dws.challenge.domain;

import lombok.Data;

@Data
public class MoneyTransferResponse {

    private String response;

    public MoneyTransferResponse(String response) {
        this.response = response;
    }
}
