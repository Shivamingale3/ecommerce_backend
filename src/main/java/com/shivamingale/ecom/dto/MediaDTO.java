package com.shivamingale.ecom.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.Media;
import com.shivamingale.ecom.enums.MediaType;
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

    public static MediaDTO fromEntity(Media media) {
        if (media == null) return null;
        return MediaDTO.builder()
            .id(media.getId())
            .name(media.getName())
            .type(media.getType())
            .url(media.hasValidPresignedUrl() ? media.getPresignedUrl() : media.getUrl())
            .altText(media.getAltText())
            .width(media.getWidth())
            .height(media.getHeight())
            .build();
    }
}