package com.window.domain.whitelist.repository;

import com.window.domain.whitelist.entity.Whitelist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhitelistRepository extends JpaRepository <Whitelist, Long> {

    boolean existsByMacAddress(String macAddress);
}
