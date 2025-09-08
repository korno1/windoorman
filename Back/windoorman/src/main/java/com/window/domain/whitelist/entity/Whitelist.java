package com.window.domain.whitelist.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Whitelist {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mac_address", nullable = false, unique = true, length = 100)
    private String macAddress;
}
