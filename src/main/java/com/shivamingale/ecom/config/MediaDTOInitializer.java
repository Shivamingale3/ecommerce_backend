package com.shivamingale.ecom.config;

import org.springframework.stereotype.Component;

import com.shivamingale.ecom.dto.response.MediaDTO;
import com.shivamingale.ecom.service.MediaService;

import jakarta.annotation.PostConstruct;

@Component
public class MediaDTOInitializer {

    private final MediaService mediaService;

    public MediaDTOInitializer(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostConstruct
    public void init() {
        MediaDTO.setMediaService(mediaService);
    }
}
