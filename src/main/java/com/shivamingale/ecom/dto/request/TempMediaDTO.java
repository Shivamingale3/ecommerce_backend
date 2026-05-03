package com.shivamingale.ecom.dto.request;

import org.springframework.web.multipart.MultipartFile;

import com.shivamingale.ecom.enums.MediaRole;
import com.shivamingale.ecom.enums.MediaType;
import com.shivamingale.ecom.validation.ValidEnum;
import com.shivamingale.ecom.validation.ValidFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TempMediaDTO {

    @ValidEnum(enumClass = MediaRole.class, message = "Invalid media role provided")
    private String role;

    @ValidEnum(enumClass = MediaType.class, message = "Invalid media type provided")
    private String type;

    @ValidFile
    private MultipartFile media;

    public MediaRole getRoleEnum() {
        return MediaRole.valueOf(role.toUpperCase());
    }

    public MediaType getTypeEnum() {
        return MediaType.valueOf(type.toUpperCase());
    }
}
