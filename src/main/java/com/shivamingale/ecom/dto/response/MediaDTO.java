package com.shivamingale.ecom.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.Media;
import com.shivamingale.ecom.enums.MediaType;
import com.shivamingale.ecom.service.MediaService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaDTO {

    private String id;
    private String name;
    private MediaType type;
    private String url;
    private String altText;
    private Integer width;
    private Integer height;

    private static MediaService mediaService;

    public static void setMediaService(MediaService mediaService) {
        MediaDTO.mediaService = mediaService;
    }

    public static MediaDTO fromEntity(Media media) {
        if (media == null)
            return null;
        return MediaDTO.builder()
                .id(media.getId())
                .name(media.getName())
                .type(media.getType())
                .url(mediaService.resolvePresignedUrl(media))
                .altText(media.getAltText())
                .width(media.getWidth())
                .height(media.getHeight())
                .build();
    }
}