package com.window.domain.windows.entity;

import com.window.domain.place.entity.Place;
import com.window.domain.windows.dto.request.WindowsToggleRequestDto;
import com.window.domain.windows.dto.request.WindowsUpdateRequestDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor
public class Windows {


    @Builder
    public Windows(Long id, Place place, String name, String deviceId, boolean isAuto) {
        this.id = id;
        this.place = place;
        this.name = name;
        this.deviceId = deviceId;
        this.isAuto = isAuto;
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @ColumnDefault("false")
    @Column(name = "is_auto", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isAuto;

    public void updateWindow(WindowsUpdateRequestDto dto){
        if(dto.getName() != null) this.name = dto.getName();
        if(dto.getDeviceId() != null) this.deviceId = dto.getDeviceId();
    }

    public void autoUpdateWindow(WindowsToggleRequestDto dto){
        this.isAuto = dto.getIsAuto();
    }

}
